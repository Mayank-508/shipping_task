package com.jumbotail.shipping.service;

import com.jumbotail.shipping.domain.DeliverySpeed;
import com.jumbotail.shipping.dto.ShippingChargeRequest;
import com.jumbotail.shipping.dto.ShippingChargeResponse;
import com.jumbotail.shipping.dto.WarehouseDto;
import com.jumbotail.shipping.entity.Customer;
import com.jumbotail.shipping.entity.Warehouse;
import com.jumbotail.shipping.exception.ResourceNotFoundException;
import com.jumbotail.shipping.repository.CustomerRepository;
import com.jumbotail.shipping.repository.WarehouseRepository;
import com.jumbotail.shipping.strategy.TransportPricingFactory;
import com.jumbotail.shipping.strategy.TransportPricingStrategy;
import com.jumbotail.shipping.util.DistanceCalculator;
import com.jumbotail.shipping.entity.Product;
import com.jumbotail.shipping.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ShippingChargeService {

    private static final BigDecimal STANDARD_BASE_CHARGE = BigDecimal.valueOf(10);
    private static final BigDecimal EXPRESS_BASE_CHARGE = BigDecimal.valueOf(10);
    private static final BigDecimal EXPRESS_EXTRA_PER_KG = BigDecimal.valueOf(1.2);

    private final TransportPricingFactory transportFactory;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;
    private final DistanceCalculator distanceCalculator;
    private final WarehouseService warehouseService;
    private final ProductRepository productRepository;

    public ShippingChargeService(TransportPricingFactory transportFactory,
            WarehouseRepository warehouseRepository,
            CustomerRepository customerRepository,
            DistanceCalculator distanceCalculator,
            WarehouseService warehouseService,
            ProductRepository productRepository) {
        this.transportFactory = transportFactory;
        this.warehouseRepository = warehouseRepository;
        this.customerRepository = customerRepository;
        this.distanceCalculator = distanceCalculator;
        this.warehouseService = warehouseService;
        this.productRepository = productRepository;
    }

    public BigDecimal calculateShippingCharge(Long warehouseId, Long customerId, DeliverySpeed speed, double weightKg) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        double distanceKm = distanceCalculator.calculateDistance(
                warehouse.getLatitude(), warehouse.getLongitude(),
                customer.getLatitude(), customer.getLongitude());

        return calculateChargeInternally(distanceKm, weightKg, speed);
    }

    public ShippingChargeResponse calculateFullRouteCharge(ShippingChargeRequest request) {
        // 1. Calculate aggregated weight of products
        List<Long> productIds = request.getProductIds();
        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new ResourceNotFoundException("One or more products not found for the given IDs.");
        }

        double totalWeightKg = products.stream()
                .mapToDouble(Product::getWeight)
                .sum();

        // Validate customer
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        // Validate seller
        Long sellerId = request.getSellerId();

        // We ensure all products belong to the specified seller here if needed.
        boolean allBelongToSeller = products.stream().allMatch(p -> p.getSeller().getId().equals(sellerId));
        if (!allBelongToSeller) {
            throw new IllegalArgumentException("Not all products belong to the specified seller.");
        }

        // We use the warehouseService which uses the first product to validate + find
        // nearest warehouse.
        // It's cached by sellerId.
        Long firstProductId = products.get(0).getId();
        WarehouseDto nearestWarehouseDto = warehouseService.findNearestWarehouse(sellerId, firstProductId);

        Warehouse nearestWarehouse = warehouseRepository.findById(nearestWarehouseDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nearest warehouse not found in DB."));

        double distanceKm = distanceCalculator.calculateDistance(
                nearestWarehouse.getLatitude(), nearestWarehouse.getLongitude(),
                customer.getLatitude(), customer.getLongitude());

        BigDecimal charge = calculateChargeInternally(distanceKm, totalWeightKg, request.getDeliverySpeed());

        return new ShippingChargeResponse(charge.setScale(2, RoundingMode.HALF_UP), nearestWarehouseDto);
    }

    private BigDecimal calculateChargeInternally(double distanceKm, double weightKg, DeliverySpeed speed) {
        TransportPricingStrategy strategy = transportFactory.getStrategy(distanceKm);
        BigDecimal transportCost = strategy.calculateTransportCost(distanceKm, weightKg);

        BigDecimal totalCharge = BigDecimal.ZERO;

        if (speed == DeliverySpeed.STANDARD) {
            totalCharge = STANDARD_BASE_CHARGE.add(transportCost);
        } else if (speed == DeliverySpeed.EXPRESS) {
            BigDecimal expressExtra = EXPRESS_EXTRA_PER_KG.multiply(BigDecimal.valueOf(weightKg));
            totalCharge = EXPRESS_BASE_CHARGE.add(expressExtra).add(transportCost);
        } else {
            throw new IllegalArgumentException("Unknown delivery speed: " + speed);
        }

        return totalCharge;
    }
}
