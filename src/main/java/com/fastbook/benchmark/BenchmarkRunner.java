package com.fastbook.benchmark;

import com.fastbook.engine.MatchingEngine;
import com.fastbook.model.Order;
import com.fastbook.model.Side;
import com.fastbook.model.Trade;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BenchmarkRunner {
    private static final String[] SYMBOLS = {"AAPL", "MSFT", "NVDA", "SPY", "QQQ"};
    private static final long BASE_PRICE = 18750;
    private static final int PRICE_SPREAD = 20; // ticks around base

    private final int orderCount;
    private final Random random;

    public BenchmarkRunner(int orderCount, long seed) {
        this.orderCount = orderCount;
        this.random = new Random(seed);
    }

    public void run() {
        Order[] orders = generateOrders();
        long[] latencies = new long[orderCount];
        int totalTrades = 0;

        MatchingEngine engine = new MatchingEngine();

        // Warmup
        MatchingEngine warmup = new MatchingEngine();
        for (int i = 0; i < Math.min(10_000, orderCount); i++) {
            warmup.submit(orders[i]);
        }

        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < orderCount; i++) {
            long t0 = System.nanoTime();
            List<Trade> trades = engine.submit(orders[i]);
            latencies[i] = System.nanoTime() - t0;
            totalTrades += trades.size();
        }
        long totalTime = System.nanoTime() - startTime;

        printResults(latencies, totalTime, totalTrades, engine);
    }

    private Order[] generateOrders() {
        Order[] orders = new Order[orderCount];
        for (int i = 0; i < orderCount; i++) {
            String symbol = SYMBOLS[random.nextInt(SYMBOLS.length)];
            Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
            long price = BASE_PRICE + random.nextInt(PRICE_SPREAD * 2) - PRICE_SPREAD;
            long quantity = (random.nextInt(10) + 1) * 10L;
            orders[i] = new Order(i + 1, symbol, side, price, quantity, i);
        }
        return orders;
    }

    private void printResults(long[] latencies, long totalTimeNs, int totalTrades, MatchingEngine engine) {
        Arrays.sort(latencies);
        double totalTimeSec = totalTimeNs / 1_000_000_000.0;
        double throughput = orderCount / totalTimeSec;

        System.out.println("=== FastBook Benchmark Results ===");
        System.out.println();
        System.out.printf("Orders processed:    %,d%n", orderCount);
        System.out.printf("Trades generated:    %,d%n", totalTrades);
        System.out.printf("Total time:          %.3f s%n", totalTimeSec);
        System.out.printf("Throughput:          %,.0f orders/sec%n", throughput);
        System.out.println();
        System.out.println("Latency (per order):");
        System.out.printf("  p50:  %,d ns%n", percentile(latencies, 50));
        System.out.printf("  p95:  %,d ns%n", percentile(latencies, 95));
        System.out.printf("  p99:  %,d ns%n", percentile(latencies, 99));
        System.out.printf("  avg:  %,d ns%n", average(latencies));
        System.out.println();
        int restingOrders = 0;
        for (String sym : SYMBOLS) {
            var book = engine.getBook(sym);
            if (book != null) restingOrders += book.orderCount();
        }
        System.out.printf("Resting orders:      %,d%n", restingOrders);
    }

    private static long percentile(long[] sorted, int p) {
        int index = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, index)];
    }

    private static long average(long[] values) {
        long sum = 0;
        for (long v : values) sum += v;
        return sum / values.length;
    }

    public static void main(String[] args) {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
        long seed = args.length > 1 ? Long.parseLong(args[1]) : 42;
        System.out.printf("Running benchmark with %,d orders (seed=%d)...%n%n", count, seed);
        new BenchmarkRunner(count, seed).run();
    }
}
