# FastBook

FastBook is a Java limit order book and matching engine built to explore the data structures and performance tradeoffs behind electronic trading systems.

## Why I Built This

I built this project to deepen my understanding of trading infrastructure, low-latency backend systems, and performance-sensitive Java. The goal was not to build a real trading bot, but to implement the core matching logic that powers exchange-style markets.

## Features

- Limit order add / cancel / modify
- FIFO matching at each price level
- Partial fills
- Best bid / best ask queries
- Multi-symbol support
- Unit tests for matching correctness

## Technical Design

### Data Structures

| Structure | Purpose |
|-----------|---------|
| `TreeMap<Long, PriceLevel>` (reverse order) | Bids sorted highest-first — `firstKey()` is always best bid |
| `TreeMap<Long, PriceLevel>` (natural order) | Asks sorted lowest-first — `firstKey()` is always best ask |
| `HashMap<Long, Order>` | O(1) order lookup for cancel/modify by ID |
| `ArrayDeque<Order>` | FIFO queue at each price level for time-priority matching |

### Price Representation

Prices are stored as integer ticks (cents) to avoid floating-point precision issues:

```java
long priceTicks = 18750; // $187.50
```

### Matching Rules

- A BUY order matches the lowest SELL price ≤ the buy price
- A SELL order matches the highest BUY price ≥ the sell price
- At the same price level, orders are matched FIFO
- Partial fills are supported — unfilled remainder rests in the book
- Fully filled orders are removed immediately

### Architecture

```
MatchingEngine
  └── Map<String, OrderBook>       (one book per symbol)
        ├── TreeMap bids
        ├── TreeMap asks
        └── HashMap ordersById
```

Orders are matched on arrival (aggressive matching). If an incoming order cannot be fully filled, its remainder is inserted into the appropriate price level.

## Tradeoffs

**Optimized for:**
- Correctness and clarity of matching logic
- O(log n) insert/match via TreeMap price levels
- O(1) cancel/modify via HashMap order index
- Clean separation between routing (MatchingEngine) and matching (OrderBook)

**Intentionally not implemented (yet):**
- Market orders, stop orders, IOC/FOK order types
- Persistent storage or event sourcing
- Network protocol or API layer
- Object pooling or GC-free allocation

## Building and Running

Requires Java 17+.

```bash
./gradlew build       # compile + run tests
./gradlew test        # tests only
./gradlew benchmark   # run synthetic order benchmark (1M orders)
```

## Performance Results

Benchmark: 1,000,000 synthetic limit orders across 5 symbols (AAPL, MSFT, NVDA, SPY, QQQ) with randomized prices, sides, and quantities.

| Metric | Value |
|--------|-------|
| Orders processed | 1,000,000 |
| Trades generated | 709,673 |
| Throughput | ~4.6M orders/sec |
| p50 latency | 125 ns |
| p95 latency | 375 ns |
| p99 latency | 1,291 ns |

Measured on a single thread with `System.nanoTime()`. Results vary by hardware.

## Future Work

- Benchmark runner with throughput and p50/p95/p99 latency reporting
- CSV event replay with deterministic trade output
- JMH microbenchmarks
- Market data snapshot publisher
- Comparison of TreeMap vs flat-array price-level indexing
- Binary protocol ingestion via TCP
