package com.fastbook.engine;

import com.fastbook.model.*;

import java.util.*;

public class OrderBook {
    private final String symbol;
    private final TreeMap<Long, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Long, PriceLevel> asks = new TreeMap<>();
    private final Map<Long, Order> ordersById = new HashMap<>();

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() { return symbol; }

    public List<Trade> addOrder(Order order) {
        if (ordersById.containsKey(order.getOrderId())) {
            throw new IllegalArgumentException("Duplicate orderId: " + order.getOrderId());
        }
        List<Trade> trades = match(order);
        if (!order.isFilled()) {
            rest(order);
        }
        return trades;
    }

    public boolean cancelOrder(long orderId) {
        Order order = ordersById.remove(orderId);
        if (order == null) return false;
        TreeMap<Long, PriceLevel> book = order.getSide() == Side.BUY ? bids : asks;
        PriceLevel level = book.get(order.getPriceTicks());
        if (level != null) {
            level.remove(order);
            if (level.isEmpty()) book.remove(order.getPriceTicks());
        }
        return true;
    }

    public List<Trade> modifyOrder(long orderId, long newQuantity) {
        Order existing = ordersById.get(orderId);
        if (existing == null) throw new IllegalArgumentException("Order not found: " + orderId);
        cancelOrder(orderId);
        Order replacement = new Order(
            orderId, existing.getSymbol(), existing.getSide(),
            existing.getPriceTicks(), newQuantity, existing.getTimestamp()
        );
        return addOrder(replacement);
    }

    public OptionalLong getBestBid() {
        return bids.isEmpty() ? OptionalLong.empty() : OptionalLong.of(bids.firstKey());
    }

    public OptionalLong getBestAsk() {
        return asks.isEmpty() ? OptionalLong.empty() : OptionalLong.of(asks.firstKey());
    }

    public int orderCount() { return ordersById.size(); }

    private List<Trade> match(Order incoming) {
        List<Trade> trades = new ArrayList<>();
        TreeMap<Long, PriceLevel> oppositeBook = incoming.getSide() == Side.BUY ? asks : bids;

        while (!incoming.isFilled() && !oppositeBook.isEmpty()) {
            Map.Entry<Long, PriceLevel> bestEntry = oppositeBook.firstEntry();
            long bestPrice = bestEntry.getKey();

            if (incoming.getSide() == Side.BUY && bestPrice > incoming.getPriceTicks()) break;
            if (incoming.getSide() == Side.SELL && bestPrice < incoming.getPriceTicks()) break;

            PriceLevel level = bestEntry.getValue();
            while (!incoming.isFilled() && !level.isEmpty()) {
                Order resting = level.peek();
                long fillQty = Math.min(incoming.getRemainingQuantity(), resting.getRemainingQuantity());

                incoming.fill(fillQty);
                resting.fill(fillQty);

                long buyId = incoming.getSide() == Side.BUY ? incoming.getOrderId() : resting.getOrderId();
                long sellId = incoming.getSide() == Side.SELL ? incoming.getOrderId() : resting.getOrderId();
                trades.add(new Trade(symbol, buyId, sellId, bestPrice, fillQty, incoming.getTimestamp()));

                if (resting.isFilled()) {
                    level.remove(resting);
                    ordersById.remove(resting.getOrderId());
                }
            }
            if (level.isEmpty()) oppositeBook.remove(bestPrice);
        }
        return trades;
    }

    private void rest(Order order) {
        TreeMap<Long, PriceLevel> book = order.getSide() == Side.BUY ? bids : asks;
        book.computeIfAbsent(order.getPriceTicks(), PriceLevel::new).add(order);
        ordersById.put(order.getOrderId(), order);
    }
}
