package com.jumbotail.shipping.strategy;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TransportPricingFactory {

    private final List<TransportPricingStrategy> strategies;

    public TransportPricingFactory(List<TransportPricingStrategy> strategies) {
        this.strategies = strategies;
    }

    public TransportPricingStrategy getStrategy(double distanceKm) {
        return strategies.stream()
                .filter(strategy -> strategy.isApplicable(distanceKm))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No suitable transport mode found for distance: " + distanceKm));
    }
}
