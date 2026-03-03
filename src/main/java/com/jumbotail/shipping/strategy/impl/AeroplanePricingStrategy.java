package com.jumbotail.shipping.strategy.impl;

import com.jumbotail.shipping.strategy.TransportPricingStrategy;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Transport strategy for distances over 500km, using an Aeroplane.
 */
@Component
public class AeroplanePricingStrategy implements TransportPricingStrategy {

    private static final double RATE_PER_KM_KG = 1.0;
    private static final double MIN_DISTANCE_KM = 500.0;

    @Override
    public boolean isApplicable(double distanceKm) {
        return distanceKm > MIN_DISTANCE_KM;
    }

    @Override
    public BigDecimal calculateTransportCost(double distanceKm, double weightKg) {
        return BigDecimal.valueOf(RATE_PER_KM_KG * distanceKm * weightKg);
    }
}
