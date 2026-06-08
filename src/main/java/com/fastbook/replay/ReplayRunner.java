package com.fastbook.replay;

import com.fastbook.engine.MatchingEngine;
import com.fastbook.model.Order;
import com.fastbook.model.Side;
import com.fastbook.model.Trade;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ReplayRunner {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: ReplayRunner <input.csv> [output.csv]");
            System.exit(1);
        }

        Path inputPath = Path.of(args[0]);
        PrintStream out = args.length > 1
            ? new PrintStream(new FileOutputStream(args[1]))
            : System.out;

        MatchingEngine engine = new MatchingEngine();
        out.println("timestamp,symbol,buyOrderId,sellOrderId,price,quantity");

        try (BufferedReader reader = Files.newBufferedReader(inputPath)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", -1);
                long timestamp = Long.parseLong(parts[0].trim());
                String symbol = parts[1].trim();
                String type = parts[2].trim();

                switch (type) {
                    case "ADD" -> {
                        Side side = Side.valueOf(parts[3].trim());
                        long price = Long.parseLong(parts[4].trim());
                        long quantity = Long.parseLong(parts[5].trim());
                        long orderId = Long.parseLong(parts[6].trim());
                        Order order = new Order(orderId, symbol, side, price, quantity, timestamp);
                        List<Trade> trades = engine.submit(order);
                        for (Trade t : trades) {
                            out.printf("%d,%s,%d,%d,%d,%d%n",
                                t.timestamp(), t.symbol(), t.buyOrderId(),
                                t.sellOrderId(), t.priceTicks(), t.quantity());
                        }
                    }
                    case "CANCEL" -> {
                        long orderId = Long.parseLong(parts[6].trim());
                        engine.cancel(symbol, orderId);
                    }
                    default -> System.err.println("Unknown type: " + type);
                }
            }
        }

        if (out != System.out) out.close();
    }
}
