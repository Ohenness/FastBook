package com.fastbook;

import com.fastbook.engine.MatchingEngine;
import com.fastbook.engine.OrderBook;
import com.fastbook.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {
    private OrderBook book;

    @BeforeEach
    void setUp() {
        book = new OrderBook("AAPL");
    }

    @Test
    void restingOrderWithNoMatch() {
        Order buy = new Order(1, "AAPL", Side.BUY, 18750, 100, 1);
        List<Trade> trades = book.addOrder(buy);
        assertTrue(trades.isEmpty());
        assertEquals(1, book.orderCount());
        assertEquals(18750, book.getBestBid().getAsLong());
        assertTrue(book.getBestAsk().isEmpty());
    }

    @Test
    void exactMatchProducesTrade() {
        book.addOrder(new Order(1, "AAPL", Side.BUY, 18750, 100, 1));
        List<Trade> trades = book.addOrder(new Order(2, "AAPL", Side.SELL, 18750, 100, 2));

        assertEquals(1, trades.size());
        Trade t = trades.get(0);
        assertEquals(1, t.buyOrderId());
        assertEquals(2, t.sellOrderId());
        assertEquals(18750, t.priceTicks());
        assertEquals(100, t.quantity());
        assertEquals(0, book.orderCount());
    }

    @Test
    void buyMatchesLowestAsk() {
        book.addOrder(new Order(1, "AAPL", Side.SELL, 18800, 100, 1));
        book.addOrder(new Order(2, "AAPL", Side.SELL, 18750, 100, 2));
        List<Trade> trades = book.addOrder(new Order(3, "AAPL", Side.BUY, 18800, 100, 3));

        assertEquals(1, trades.size());
        assertEquals(2, trades.get(0).sellOrderId()); // matched the cheaper ask
        assertEquals(18750, trades.get(0).priceTicks());
    }

    @Test
    void sellMatchesHighestBid() {
        book.addOrder(new Order(1, "AAPL", Side.BUY, 18700, 100, 1));
        book.addOrder(new Order(2, "AAPL", Side.BUY, 18750, 100, 2));
        List<Trade> trades = book.addOrder(new Order(3, "AAPL", Side.SELL, 18700, 100, 3));

        assertEquals(1, trades.size());
        assertEquals(2, trades.get(0).buyOrderId()); // matched the higher bid
        assertEquals(18750, trades.get(0).priceTicks());
    }

    @Test
    void partialFillLeavesRemainder() {
        book.addOrder(new Order(1, "AAPL", Side.BUY, 18750, 100, 1));
        List<Trade> trades = book.addOrder(new Order(2, "AAPL", Side.SELL, 18750, 60, 2));

        assertEquals(1, trades.size());
        assertEquals(60, trades.get(0).quantity());
        assertEquals(1, book.orderCount()); // buy still resting with 40 remaining
        assertEquals(18750, book.getBestBid().getAsLong());
    }

    @Test
    void incomingOrderSweepsMultipleLevels() {
        book.addOrder(new Order(1, "AAPL", Side.SELL, 18750, 50, 1));
        book.addOrder(new Order(2, "AAPL", Side.SELL, 18760, 50, 2));
        List<Trade> trades = book.addOrder(new Order(3, "AAPL", Side.BUY, 18760, 100, 3));

        assertEquals(2, trades.size());
        assertEquals(50, trades.get(0).quantity());
        assertEquals(18750, trades.get(0).priceTicks());
        assertEquals(50, trades.get(1).quantity());
        assertEquals(18760, trades.get(1).priceTicks());
        assertEquals(0, book.orderCount());
    }

    @Test
    void fifoMatchingAtSamePriceLevel() {
        book.addOrder(new Order(1, "AAPL", Side.SELL, 18750, 50, 1));
        book.addOrder(new Order(2, "AAPL", Side.SELL, 18750, 50, 2));
        List<Trade> trades = book.addOrder(new Order(3, "AAPL", Side.BUY, 18750, 50, 3));

        assertEquals(1, trades.size());
        assertEquals(1, trades.get(0).sellOrderId()); // first-in gets matched first
    }

    @Test
    void cancelRemovesOrder() {
        book.addOrder(new Order(1, "AAPL", Side.BUY, 18750, 100, 1));
        assertTrue(book.cancelOrder(1));
        assertEquals(0, book.orderCount());
        assertTrue(book.getBestBid().isEmpty());
    }

    @Test
    void cancelNonexistentReturnsFalse() {
        assertFalse(book.cancelOrder(999));
    }

    @Test
    void modifyOrderChangesQuantity() {
        book.addOrder(new Order(1, "AAPL", Side.BUY, 18750, 100, 1));
        book.modifyOrder(1, 200);
        // order re-added at same price with new qty — match against it
        List<Trade> trades = book.addOrder(new Order(2, "AAPL", Side.SELL, 18750, 200, 2));
        assertEquals(1, trades.size());
        assertEquals(200, trades.get(0).quantity());
    }

    @Test
    void noMatchWhenPricesDontCross() {
        book.addOrder(new Order(1, "AAPL", Side.BUY, 18700, 100, 1));
        List<Trade> trades = book.addOrder(new Order(2, "AAPL", Side.SELL, 18750, 100, 2));
        assertTrue(trades.isEmpty());
        assertEquals(2, book.orderCount());
        assertEquals(18700, book.getBestBid().getAsLong());
        assertEquals(18750, book.getBestAsk().getAsLong());
    }

    @Test
    void duplicateOrderIdThrows() {
        book.addOrder(new Order(1, "AAPL", Side.BUY, 18750, 100, 1));
        assertThrows(IllegalArgumentException.class, () ->
            book.addOrder(new Order(1, "AAPL", Side.SELL, 18750, 100, 2))
        );
    }
}

class MatchingEngineTest {
    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingEngine();
    }

    @Test
    void multiSymbolIsolation() {
        engine.submit(new Order(1, "AAPL", Side.BUY, 18750, 100, 1));
        engine.submit(new Order(2, "MSFT", Side.BUY, 42000, 50, 2));

        List<Trade> trades = engine.submit(new Order(3, "AAPL", Side.SELL, 18750, 100, 3));
        assertEquals(1, trades.size());
        assertEquals("AAPL", trades.get(0).symbol());

        // MSFT order unaffected
        assertEquals(1, engine.getBook("MSFT").orderCount());
    }

    @Test
    void cancelAcrossSymbols() {
        engine.submit(new Order(1, "AAPL", Side.BUY, 18750, 100, 1));
        engine.submit(new Order(2, "MSFT", Side.BUY, 42000, 50, 2));

        assertTrue(engine.cancel("AAPL", 1));
        assertFalse(engine.cancel("AAPL", 2)); // wrong symbol
        assertEquals(0, engine.getBook("AAPL").orderCount());
        assertEquals(1, engine.getBook("MSFT").orderCount());
    }
}
