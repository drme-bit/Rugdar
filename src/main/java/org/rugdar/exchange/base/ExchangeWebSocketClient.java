package org.rugdar.exchange.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.rugdar.dto.Ticker;
import org.rugdar.utils.Log;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

public abstract class ExchangeWebSocketClient implements DisposableBean {

    private static final Logger log = Log.get(ExchangeWebSocketClient.class);
    private static final int PING_INTERVAL_SECONDS = 20;

    private final String url;
    protected final ObjectMapper mapper;
    private final StandardWebSocketClient client;
    private final ApplicationEventPublisher publisher;
    private final ExponentialBackoffReconnectPolicy reconnectPolicy = new ExponentialBackoffReconnectPolicy();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "ws-scheduler"));

    private volatile WebSocketSession session;
    private volatile boolean closed;
    private int reconnectAttempts;
    private ScheduledFuture<?> keepAliveTask;

    protected ExchangeWebSocketClient(String url, ObjectMapper mapper, ApplicationEventPublisher publisher) {
        this.url = url;
        this.mapper = mapper;
        this.client = new StandardWebSocketClient();
        this.publisher = publisher;
    }

    public void connect() {
        if (closed) {
            return;
        }
        try {
            client.execute(new ExchangeWebSocketClientHandler(this), url);
        } catch (Exception e) {
            log.error("WS connect error", e);
            scheduleReconnect();
        }
    }

    void onConnected(WebSocketSession webSocketSession) {
        session = webSocketSession;
        reconnectAttempts = 0;
        log.info("Connected to {}", url);
        startKeepAlive();
        subscribe();
    }

    void onMessage(String payload) {
        dispatch(payload);
    }

    void onDisconnected() {
        session = null;
        stopKeepAlive();
        scheduleReconnect();
    }

    protected void send(String text) {
        WebSocketSession current = session;
        if (current != null && current.isOpen()) {
            try {
                current.sendMessage(new TextMessage(text));
            } catch (Exception e) {
                log.warn("WS send error", e);
            }
        }
    }

    protected void sendJson(JsonNode message) {
        try {
            send(mapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize WS message", e);
        }
    }

    protected void sendPingFrame() {
        WebSocketSession current = session;
        if (current != null && current.isOpen()) {
            try {
                current.sendMessage(new PingMessage(ByteBuffer.wrap(new byte[0])));
            } catch (Exception e) {
                log.warn("WS ping frame send error", e);
            }
        }
    }

    protected Optional<JsonNode> readTree(String raw) {
        try {
            return Optional.of(mapper.readTree(raw));
        } catch (Exception e) {
            log.warn("WS message parse error", e);
            return Optional.empty();
        }
    }

    protected abstract Optional<Ticker> parseTicker(String raw);

    protected abstract void subscribe();

    protected abstract void ping();

    private void dispatch(String raw) {
        parseTicker(raw).ifPresent(publisher::publishEvent);
    }

    private void startKeepAlive() {
        stopKeepAlive();
        keepAliveTask = scheduler.scheduleAtFixedRate(
                this::ping, PING_INTERVAL_SECONDS, PING_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void stopKeepAlive() {
        if (keepAliveTask != null) {
            keepAliveTask.cancel(true);
            keepAliveTask = null;
        }
    }

    private void scheduleReconnect() {
        if (closed) {
            return;
        }
        reconnectAttempts++;
        if (reconnectPolicy.shouldGiveUp(reconnectAttempts)) {
            log.error("Giving up after {} reconnect attempts to {}", reconnectAttempts, url);
            return;
        }
        long delay = reconnectPolicy.nextDelaySeconds(reconnectAttempts);
        log.info("Reconnecting to {} in {}s (attempt {}/{})", url, delay, reconnectAttempts, reconnectPolicy.maxAttempts());
        scheduler.schedule(this::connect, delay, TimeUnit.SECONDS);
    }

    @Override
    public void destroy() {
        closed = true;
        WebSocketSession current = session;
        if (current != null && current.isOpen()) {
            try {
                current.close();
            } catch (Exception e) {
                log.warn("WS close error", e);
            }
        }
        stopKeepAlive();
        scheduler.shutdownNow();
    }
}
