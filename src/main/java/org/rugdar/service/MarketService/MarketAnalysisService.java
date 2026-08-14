package org.rugdar.service.MarketService;

import org.rugdar.utils.Log;
import org.rugdar.ws.MarketDataWsHandler;
import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MarketAnalysisService {

    private static final Logger log = Log.get(MarketAnalysisService.class);
    private final boolean analysisEnabled;
    private final String systemPrompt;

    private final ChatClient chatClient;
    private final SnapshotService snapshots;
    private final MarketDataWsHandler wsHandler;

    public MarketAnalysisService(
            @Value("rugdar.ai.system-prompt") String systemPrompt,
            @Value("${rugdar.ai.enabled}") boolean analysisEnabled,
            ChatClient.Builder chatClientBuilder,
            SnapshotService snapshots,
            MarketDataWsHandler wsHandler) {
        this.chatClient = chatClientBuilder.build();
        this.snapshots = snapshots;
        this.wsHandler = wsHandler;

        this.systemPrompt = systemPrompt;
        this.analysisEnabled = analysisEnabled;
    }

    @Scheduled(fixedRateString = "${rugdar.analysis.interval-seconds:30}000")
    public void analyzeCurrentMarket() {
        try {
            String snapshot = snapshots.currentJson();
            log.info("Analyzing market snapshot ({} bytes)", snapshot.length());
            String analysis = chatClient.prompt()
                    .system(this.systemPrompt)
                    .user(snapshot)
                    .call()
                    .content();
            wsHandler.broadcast("analysis", analysis);
        } catch (Exception e) {
            log.warn("Market analysis failed", e);
        }
    }
}
