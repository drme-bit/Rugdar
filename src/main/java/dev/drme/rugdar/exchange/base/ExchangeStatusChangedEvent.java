package dev.drme.rugdar.exchange.base;

public record ExchangeStatusChangedEvent(String exchange, ConnectionState state) {
}
