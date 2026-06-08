package com.fastbook.model;

public class Order {
    private final long orderId;
    private final String symbol;
    private final Side side;
    private final long priceTicks;
    private long remainingQuantity;
    private final long timestamp;

    public Order(long orderId, String symbol, Side side, long priceTicks, long quantity, long timestamp) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.priceTicks = priceTicks;
        this.remainingQuantity = quantity;
        this.timestamp = timestamp;
    }

    public long getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public Side getSide() { return side; }
    public long getPriceTicks() { return priceTicks; }
    public long getRemainingQuantity() { return remainingQuantity; }
    public long getTimestamp() { return timestamp; }
    public boolean isFilled() { return remainingQuantity == 0; }

    public void fill(long quantity) {
        if (quantity > remainingQuantity) {
            throw new IllegalArgumentException("Fill quantity exceeds remaining");
        }
        this.remainingQuantity -= quantity;
    }
}
