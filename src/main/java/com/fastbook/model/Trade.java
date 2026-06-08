package com.fastbook.model;

public record Trade(
    String symbol,
    long buyOrderId,
    long sellOrderId,
    long priceTicks,
    long quantity,
    long timestamp
) {}
