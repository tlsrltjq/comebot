package com.giseop.comebot.market.websocket;

import java.net.URI;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;

interface WebSocketConnector {

    CompletableFuture<WebSocket> connect(URI uri, WebSocket.Listener listener);
}
