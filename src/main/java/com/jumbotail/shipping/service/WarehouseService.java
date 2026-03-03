package com.jumbotail.shipping.service;

import com.jumbotail.shipping.dto.WarehouseDto;
import com.jumbotail.shipping.entity.Product;
import com.jumbotail.shipping.entity.Seller;
import com.jumbotail.shipping.entity.Warehouse;
import com.jumbotail.shipping.exception.ResourceNotFoundException;
import com.jumbotail.shipping.repository.ProductRepository;
import com.jumbotail.shipping.repository.SellerRepository;
import com.jumbotail.shipping.repository.WarehouseRepository;
import com.jumbotail.shipping.util.DistanceCalculator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final DistanceCalculator distanceCalculator;

    public WarehouseService(WarehouseRepository warehouseRepository,
            SellerRepository sellerRepository,
            ProductRepository productRepository,
            DistanceCalculator distanceCalculator) {
        this.warehouseRepository = warehouseRepository;
        this.sellerRepository = sellerRepository;
        this.productRepository = productRepository;
        this.distanceCalculator = distanceCalculator;
    }

    @Cacheable(value = "nearestWarehouse", key = "#sellerId")
    public WarehouseDto findNearestWarehouse(Long sellerId, Long productId) {
        // 1. Validate Seller exists
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

        // 2. Validate Product exists and belongs to the seller
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Product does not belong to the given seller.");
        }

        // 3. Find nearest warehouse
        List<Warehouse> warehouses = warehouseRepository.findAll();
        if (warehouses.isEmpty()) {
            throw new ResourceNotFoundException("No warehouses available in the system.");
        }

        Warehouse nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Warehouse w : warehouses) {
            double distance = distanceCalculator.calculateDistance(
                    seller.getLatitude(), seller.getLongitude(),
                    w.getLatitude(), w.getLongitude());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = w;
            }
        }

        return new WarehouseDto(nearest.getId(), nearest.getLatitude(), nearest.getLongitude());
    }
}
