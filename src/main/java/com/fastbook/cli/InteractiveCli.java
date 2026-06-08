package com.fastbook.cli;

import com.fastbook.engine.MatchingEngine;
import com.fastbook.engine.OrderBook;
import com.fastbook.model.Order;
import com.fastbook.model.Side;
import com.fastbook.model.Trade;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class InteractiveCli {

    public static void main(String[] args) throws Exception {
        MatchingEngine engine = new MatchingEngine();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        long nextId = 1;

        System.out.println("FastBook Interactive CLI");
        System.out.println("Commands: ADD <symbol> <BUY|SELL> <price> <qty>");
        System.out.println("          CANCEL <symbol> <orderId>");
        System.out.println("          BOOK <symbol>");
        System.out.println("          QUIT");
        System.out.println();

        String line;
        System.out.print("> ");
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) { System.out.print("> "); continue; }

            String[] parts = line.split("\\s+");
            try {
                switch (parts[0].toUpperCase()) {
                    case "ADD" -> {
                        String symbol = parts[1].toUpperCase();
                        Side side = Side.valueOf(parts[2].toUpperCase());
                        long price = Long.parseLong(parts[3]);
                        long qty = Long.parseLong(parts[4]);
                        long id = nextId++;
                        Order order = new Order(id, symbol, side, price, qty, System.nanoTime());
                        List<Trade> trades = engine.submit(order);
                        System.out.printf("  Order %d added%n", id);
                        for (Trade t : trades) {
                            System.out.printf("  TRADE: %s buy=%d sell=%d price=%d qty=%d%n",
                                t.symbol(), t.buyOrderId(), t.sellOrderId(), t.priceTicks(), t.quantity());
                        }
                    }
                    case "CANCEL" -> {
                        String symbol = parts[1].toUpperCase();
                        long orderId = Long.parseLong(parts[2]);
                        boolean ok = engine.cancel(symbol, orderId);
                        System.out.println(ok ? "  Cancelled" : "  Not found");
                    }
                    case "BOOK" -> {
                        String symbol = parts[1].toUpperCase();
                        OrderBook book = engine.getBook(symbol);
                        if (book == null) { System.out.println("  No book for " + symbol); break; }
                        var bid = book.getBestBid();
                        var ask = book.getBestAsk();
                        System.out.printf("  %s  bid=%s  ask=%s  orders=%d%n",
                            symbol,
                            bid.isPresent() ? String.valueOf(bid.getAsLong()) : "-",
                            ask.isPresent() ? String.valueOf(ask.getAsLong()) : "-",
                            book.orderCount());
                    }
                    case "QUIT", "EXIT" -> { return; }
                    default -> System.out.println("  Unknown command: " + parts[0]);
                }
            } catch (Exception e) {
                System.out.println("  Error: " + e.getMessage());
            }
            System.out.print("> ");
        }
    }
}
