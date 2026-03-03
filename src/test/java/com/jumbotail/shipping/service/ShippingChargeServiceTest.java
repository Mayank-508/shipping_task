package com.jumbotail.shipping.service;

import com.jumbotail.shipping.domain.DeliverySpeed;
import com.jumbotail.shipping.dto.ShippingChargeRequest;
import com.jumbotail.shipping.dto.ShippingChargeResponse;
import com.jumbotail.shipping.dto.WarehouseDto;
import com.jumbotail.shipping.entity.Customer;
import com.jumbotail.shipping.entity.Product;
import com.jumbotail.shipping.entity.Seller;
import com.jumbotail.shipping.entity.Warehouse;
import com.jumbotail.shipping.exception.ResourceNotFoundException;
import com.jumbotail.shipping.repository.CustomerRepository;
import com.jumbotail.shipping.repository.ProductRepository;
import com.jumbotail.shipping.repository.WarehouseRepository;
import com.jumbotail.shipping.strategy.TransportPricingFactory;
import com.jumbotail.shipping.strategy.impl.AeroplanePricingStrategy;
import com.jumbotail.shipping.strategy.impl.MiniVanPricingStrategy;
import com.jumbotail.shipping.strategy.impl.TruckPricingStrategy;
import com.jumbotail.shipping.util.DistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingChargeServiceTest {

    @Mock
    private TransportPricingFactory transportFactory;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private DistanceCalculator distanceCalculator;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ShippingChargeService shippingChargeService;

    private Warehouse mockWarehouse;
    private Customer mockCustomer;
    private Seller mockSeller;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        mockWarehouse = new Warehouse(1L, 12.9716, 77.5946);
        mockCustomer = new Customer(1L, "Test User", "123", 12.2958, 76.6394);
        mockSeller = new Seller(1L, "Test Seller", 13.0, 77.0);
        product1 = new Product(1L, mockSeller, 2.5, "10x10");
        product2 = new Product(2L, mockSeller, 3.5, "15x15"); // Total weight = 6.0kg
    }

    @Test
    void calculateShippingCharge_StandardSpeed_MiniVan_Boundary100km() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(mockWarehouse));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(100.0);
        when(transportFactory.getStrategy(100.0)).thenReturn(new MiniVanPricingStrategy());

        // Standard base(10) + MiniVan(3 * 100 * 1.0) = 310
        BigDecimal actual = shippingChargeService.calculateShippingCharge(1L, 1L, DeliverySpeed.STANDARD, 1.0);
        assertEquals(310.00, actual.doubleValue(), 0.01);
    }

    @Test
    void calculateShippingCharge_ExpressSpeed_Truck_Boundary500km() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(mockWarehouse));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(500.0);
        when(transportFactory.getStrategy(500.0)).thenReturn(new TruckPricingStrategy());

        // Express base(10) + ExpressExtra(1.2 * 2.0kg) + Truck(2 * 500 * 2.0)
        // 10 + 2.4 + 2000 = 2012.4
        BigDecimal actual = shippingChargeService.calculateShippingCharge(1L, 1L, DeliverySpeed.EXPRESS, 2.0);
        assertEquals(2012.40, actual.doubleValue(), 0.01);
    }

    @Test
    void calculateShippingCharge_StandardSpeed_Aeroplane_Over500km() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(mockWarehouse));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));
        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(600.0);
        when(transportFactory.getStrategy(600.0)).thenReturn(new AeroplanePricingStrategy());

        // Standard base(10) + Aeroplane(1 * 600 * 5.0) = 3010.0
        BigDecimal actual = shippingChargeService.calculateShippingCharge(1L, 1L, DeliverySpeed.STANDARD, 5.0);
        assertEquals(3010.00, actual.doubleValue(), 0.01);
    }

    @Test
    void calculateShippingCharge_EntityNotFound() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> shippingChargeService.calculateShippingCharge(99L, 1L, DeliverySpeed.STANDARD, 1.0));
    }

    @Test
    void calculateFullRouteCharge_MultipleProducts_AggregatesWeight() {
        ShippingChargeRequest request = new ShippingChargeRequest(1L, 1L, DeliverySpeed.STANDARD,
                Arrays.asList(1L, 2L));

        when(productRepository.findAllById(request.getProductIds())).thenReturn(Arrays.asList(product1, product2));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));
        when(warehouseService.findNearestWarehouse(1L, 1L)).thenReturn(new WarehouseDto(1L, 12.0, 77.0));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(mockWarehouse));

        when(distanceCalculator.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(50.0);
        when(transportFactory.getStrategy(50.0)).thenReturn(new MiniVanPricingStrategy());

        // Weight = 2.5 + 3.5 = 6.0kg
        // Dist = 50km
        // Standard(10) + MiniVan(3 * 50 * 6.0) = 10 + 900 = 910.0
        ShippingChargeResponse response = shippingChargeService.calculateFullRouteCharge(request);

        assertEquals(910.00, response.getShippingCharge().doubleValue(), 0.01);
        assertEquals(1L, response.getNearestWarehouse().getId());
    }

    @Test
    void calculateFullRouteCharge_ProductNotFound() {
        ShippingChargeRequest request = new ShippingChargeRequest(1L, 1L, DeliverySpeed.STANDARD,
                Arrays.asList(1L, 99L));
        // Only returns 1 product
        when(productRepository.findAllById(request.getProductIds())).thenReturn(Collections.singletonList(product1));

        assertThrows(ResourceNotFoundException.class, () -> shippingChargeService.calculateFullRouteCharge(request));
    }

    @Test
    void calculateFullRouteCharge_NotAllProductsBelongToSeller() {
        Seller otherSeller = new Seller(2L, "Other", 0, 0);
        Product productOther = new Product(3L, otherSeller, 1.0, "1x1");

        ShippingChargeRequest request = new ShippingChargeRequest(1L, 1L, DeliverySpeed.STANDARD,
                Arrays.asList(1L, 3L));
        when(productRepository.findAllById(request.getProductIds())).thenReturn(Arrays.asList(product1, productOther));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));

        assertThrows(IllegalArgumentException.class, () -> shippingChargeService.calculateFullRouteCharge(request));
    }

    @Test
    void calculateFullRouteCharge_NearestWarehouseNotFoundInDB() {
        ShippingChargeRequest request = new ShippingChargeRequest(1L, 1L, DeliverySpeed.STANDARD, Arrays.asList(1L));

        when(productRepository.findAllById(request.getProductIds())).thenReturn(Collections.singletonList(product1));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));
        when(warehouseService.findNearestWarehouse(1L, 1L)).thenReturn(new WarehouseDto(99L, 12.0, 77.0));

        // Mock fail to fetch warehouse entity from DB (data anomaly)
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shippingChargeService.calculateFullRouteCharge(request));
    }
}
