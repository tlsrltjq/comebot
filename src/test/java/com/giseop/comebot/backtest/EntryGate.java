package com.giseop.comebot.backtest;

/**
 * Optional regime filter applied at entry time: returns whether a SELECTED signal
 * for {@code market} at {@code entryTimeSec} is allowed to become a position. Lets
 * regime experiments (BTC trend, recent return, volatility, breadth) gate the
 * unchanged V1 entry signal without touching the scanner.
 */
@FunctionalInterface
interface EntryGate {

    EntryGate ALLOW_ALL = (market, entryTimeSec) -> true;

    boolean allows(String market, long entryTimeSec);
}
