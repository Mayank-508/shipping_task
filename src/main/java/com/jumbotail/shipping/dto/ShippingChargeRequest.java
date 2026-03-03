package com.jumbotail.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.jumbotail.shipping.domain.DeliverySpeed;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingChargeRequest {
    @NotNull(message = "Seller ID cannot be null")
    private Long sellerId;

    @NotNull(message = "Customer ID cannot be null")
    private Long customerId;

    @NotNull(message = "Delivery Speed cannot be null")
    private DeliverySpeed deliverySpeed;

    @NotEmpty(message = "Product IDs cannot be empty")
    private List<Long> productIds;
}
