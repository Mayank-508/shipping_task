package com.jumbotail.shipping.strategy;

import java.math.BigDecimal;

public interface TransportPricingStrategy {
    boolean isApplicable(double distanceKm);

    BigDecimal calculateTransportCost(double distanceKm, double weightKg);
}
