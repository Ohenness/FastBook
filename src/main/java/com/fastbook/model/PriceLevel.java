package com.fastbook.model;

import java.util.ArrayDeque;
import java.util.Deque;

public class PriceLevel {
    private final long priceTicks;
    private final Deque<Order> orders = new ArrayDeque<>();

    public PriceLevel(long priceTicks) {
        this.priceTicks = priceTicks;
    }

    public long getPriceTicks() { return priceTicks; }
    public boolean isEmpty() { return orders.isEmpty(); }
    public Order peek() { return orders.peek(); }

    public void add(Order order) { orders.addLast(order); }

    public void remove(Order order) { orders.remove(order); }

    public long totalQuantity() {
        long total = 0;
        for (Order o : orders) {
            total += o.getRemainingQuantity();
        }
        return total;
    }
}
