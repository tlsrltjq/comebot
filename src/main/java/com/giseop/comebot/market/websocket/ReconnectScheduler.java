package com.giseop.comebot.market.websocket;

interface ReconnectScheduler {

    void schedule(Runnable task, long delayMs);
}
