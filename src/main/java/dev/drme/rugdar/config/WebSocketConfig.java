package dev.drme.rugdar.config;

import dev.drme.rugdar.ws.MarketAnalysisHandler;
import dev.drme.rugdar.ws.MarketDataHandler;
import dev.drme.rugdar.ws.MarketStatusHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MarketDataHandler marketDataHandler;
    private final MarketAnalysisHandler marketAnalysisHandler;
    private final MarketStatusHandler marketStatusHandler;

    public WebSocketConfig(MarketDataHandler marketDataHandler, MarketAnalysisHandler marketAnalysisHandler, MarketStatusHandler marketStatusHandler) {
        this.marketDataHandler = marketDataHandler;
        this.marketAnalysisHandler = marketAnalysisHandler;
        this.marketStatusHandler = marketStatusHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketDataHandler, "/ws/market").setAllowedOrigins("*");
        registry.addHandler(marketAnalysisHandler, "/ws/analysis").setAllowedOrigins("*");
        registry.addHandler(marketStatusHandler, "/ws/status").setAllowedOrigins("*");
    }
}
