package com.jumbotail.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingChargeResponse {
    private BigDecimal shippingCharge;
    private WarehouseDto nearestWarehouse;
}
