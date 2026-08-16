# Rugdar

Crypto market data aggregator. Connects to multiple exchanges over WebSocket, stores 24-hour tickers in memory, streams them to clients over its own WebSocket endpoint, and periodically generates AI-powered market analysis.

## Features

- **Multi-exchange ticker collection** over WebSocket: Binance, Bybit, Whitebit
- **In-memory storage**: latest ticker plus history (up to 200 entries) per symbol
- **Client-facing WebSocket** (`/ws/market`): subscribe to tickers and AI analysis
- **AI market analysis**: a market snapshot is sent to an LLM (Spring AI), results are broadcast to subscribers
- **Resilient connections**: exponential backoff reconnect (5s → 60s, up to 10 attempts) + keep-alive ping
- **Custom UUIDv8 generator**: time-ordered IDs (timestamp + sequence + node id)

## Tech stack

| Component | Version |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.0 |
| Spring AI | 2.0.0 |
| Spring WebSocket | server + client |
| Jackson (databind, jsr310) | — |
| Maven | — |

## Project structure

```
src/main/java/org/rugdar/
├── Main.java                      # Spring Boot entry point
├── config/                        # WebSocket, Jackson, Async configuration
├── exchange/
│   ├── ExchangeGateway.java       # Connects all exchange clients on startup
│   ├── base/                      # Abstract WS client, reconnect policy
│   └── clients/                   # Binance, Bybit, Whitebit
├── service/
│   ├── IdService.java             # UUIDv8 (time + seq + node)
│   └── MarketService/
│       ├── MarketDataService.java    # Ticker & history storage
│       ├── SnapshotService.java      # Market snapshot cache for AI analysis
│       └── MarketAnalysisService.java # Periodic AI analysis
├── dto/                           # Ticker, Analysis
├── ws/                            # MarketDataWsHandler — server-side WebSocket
└── utils/                         # Log, JsonUtils
```

## Running

Requires **JDK 25** and Maven.

```bash
# 1. Set up environment variables (or copy .env.example → .env)
export AI_GATEWAY_API_KEY=<your-api-key>

# 2. Run
mvn spring-boot:run
```

The server listens on `http://localhost:8081`.

## Configuration (`src/main/resources/application.yaml`)

```yaml
rugdar:
  ai:
    enabled: true                 # enable/disable AI analysis
    system-prompt: "..."          # analyst system prompt
  analysis:
    interval-seconds: 30          # analysis interval, seconds
  exchanges:
    binance:
      url: wss://stream.binance.com:9443/ws
      symbols: BTCUSDT,ETHUSDT
    bybit:
      url: wss://stream.bybit.com/v5/public/spot
      symbols: BTCUSDT,ETHUSDT
    whitebit:
      url: wss://api.whitebit.com/ws
      symbols: BTC_USDT,ETH_USDT
  id:
    node-id: 0                    # node id for UUIDv8 (0-1023)

spring:
  ai:
    openai:
      api-key: ${AI_GATEWAY_API_KEY}
      base-url: https://ai-gateway.vercel.sh/v1
      chat:
        model: deepseek/deepseek-v4-pro-0813

server:
  port: 8081
```

## WebSocket API

Connect to: `ws://localhost:8081/ws/market`

### Client → server

Subscribe to a topic:

```json
{ "action": "subscribe", "topic": "ticker" }
```

Unsubscribe:

```json
{ "action": "unsubscribe", "topic": "ticker" }
```

Supported topics: `ticker`, `analysis`.

Control command responses:

```json
{ "v": 1, "type": "subscribed", "seq": 1, "topic": "ticker" }
{ "v": 1, "type": "unsubscribed", "seq": 2, "topic": "ticker" }
{ "v": 1, "type": "error", "seq": 3, "code": "BAD_REQUEST", "message": "missing 'topic'" }
```

Error codes: `BAD_REQUEST`, `UNKNOWN_TOPIC`, `UNKNOWN_ACTION`.

### Server → client

Ticker push:

```json
{
  "v": 1,
  "type": "ticker",
  "seq": 4,
  "data": {
    "id": "8f7c1a3b-...",
    "exchange": "binance",
    "symbol": "BTCUSDT",
    "lastPrice": 12345.67,
    "open": 12000.00,
    "high": 12500.00,
    "low": 11900.00,
    "volume": 1500.5,
    "turnover": 18400000.0,
    "timestamp": "2026-08-16T07:30:00Z"
  }
}
```

AI analysis push:

```json
{
  "v": 1,
  "type": "analysis",
  "seq": 5,
  "data": {
    "aid": "8f7c1a3b-...",
    "model": "deepseek/deepseek-v4-pro-0813",
    "timestamp": "2026-08-16T07:30:00Z",
    "message": "Brief market overview..."
  }
}
```

Envelope fields: `v` — protocol version (1), `type` — event type, `seq` — monotonically increasing sequence number, `data` — payload.

## Tests

```bash
mvn test
```

Covered: exchange WebSocket clients (Binance, Bybit, Whitebit), gateway, reconnect policy, MarketDataService, IdService, and the server-side WS handler.

## GitHub Actions

- **CI**: build and tests on every PR
- **Qodana**: static analysis (`qodana.yaml`)
