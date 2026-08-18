package dev.drme.rugdar.config;

import dev.drme.rugdar.ws.MarketAnalysisHandler;
import dev.drme.rugdar.ws.MarketDataHandler;
import dev.drme.rugdar.ws.MarketStatusHandler;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MarketDataHandler marketDataHandler;
    private final @Nullable MarketAnalysisHandler marketAnalysisHandler;
    private final MarketStatusHandler marketStatusHandler;
    @Value("${rugdar.security.origin}") String origin;

    public WebSocketConfig(MarketDataHandler marketDataHandler,
                           @Nullable MarketAnalysisHandler marketAnalysisHandler,
                           MarketStatusHandler marketStatusHandler) {
        this.marketDataHandler = marketDataHandler;
        this.marketAnalysisHandler = marketAnalysisHandler;
        this.marketStatusHandler = marketStatusHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketDataHandler, "/ws/market").setAllowedOrigins(origin);
        if (marketAnalysisHandler != null) {
            registry.addHandler(marketAnalysisHandler, "/ws/analysis").setAllowedOrigins(origin);
        }
        registry.addHandler(marketStatusHandler, "/ws/status").setAllowedOrigins(origin);
    }
}
