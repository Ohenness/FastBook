package com.fastbook.engine;

import com.fastbook.model.*;

import java.util.*;

public class MatchingEngine {
    private final Map<String, OrderBook> books = new HashMap<>();

    public List<Trade> submit(Order order) {
        OrderBook book = books.computeIfAbsent(order.getSymbol(), OrderBook::new);
        return book.addOrder(order);
    }

    public boolean cancel(String symbol, long orderId) {
        OrderBook book = books.get(symbol);
        return book != null && book.cancelOrder(orderId);
    }

    public List<Trade> modify(String symbol, long orderId, long newQuantity) {
        OrderBook book = books.get(symbol);
        if (book == null) throw new IllegalArgumentException("No book for symbol: " + symbol);
        return book.modifyOrder(orderId, newQuantity);
    }

    public OrderBook getBook(String symbol) {
        return books.get(symbol);
    }

    public Set<String> getSymbols() {
        return Collections.unmodifiableSet(books.keySet());
    }
}
