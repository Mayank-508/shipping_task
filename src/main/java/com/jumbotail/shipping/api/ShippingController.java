package com.jumbotail.shipping.api;

import com.jumbotail.shipping.domain.DeliverySpeed;
import com.jumbotail.shipping.dto.ShippingChargeRequest;
import com.jumbotail.shipping.dto.ShippingChargeResponse;
import com.jumbotail.shipping.dto.WarehouseDto;
import com.jumbotail.shipping.service.ShippingChargeService;
import com.jumbotail.shipping.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1")
public class ShippingController {

    private final WarehouseService warehouseService;
    private final ShippingChargeService shippingChargeService;

    public ShippingController(WarehouseService warehouseService, ShippingChargeService shippingChargeService) {
        this.warehouseService = warehouseService;
        this.shippingChargeService = shippingChargeService;
    }

    // 1) GET /api/v1/warehouse/nearest
    @GetMapping("/warehouse/nearest")
    public ResponseEntity<WarehouseDto> getNearestWarehouse(@RequestParam Long sellerId, @RequestParam Long productId) {
        WarehouseDto nearest = warehouseService.findNearestWarehouse(sellerId, productId);
        return ResponseEntity.ok(nearest);
    }

    // 2) GET /api/v1/shipping-charge
    @GetMapping("/shipping-charge")
    public ResponseEntity<BigDecimal> getShippingCharge(
            @RequestParam Long warehouseId,
            @RequestParam Long customerId,
            @RequestParam DeliverySpeed deliverySpeed) {

        // Assuming weight is 1.0kg for this endpoint if not specified in the
        // assignment.
        // A real system would compute total weight from cart/products.
        BigDecimal charge = shippingChargeService.calculateShippingCharge(warehouseId, customerId, deliverySpeed, 1.0);
        return ResponseEntity.ok(charge);
    }

    // 3) POST /api/v1/shipping-charge/calculate
    @PostMapping("/shipping-charge/calculate")
    public ResponseEntity<ShippingChargeResponse> calculateFullShippingCharge(
            @Valid @RequestBody ShippingChargeRequest request) {
        ShippingChargeResponse response = shippingChargeService.calculateFullRouteCharge(request);
        return ResponseEntity.ok(response);
    }
    // Add any future Api endpoints if required.
}
