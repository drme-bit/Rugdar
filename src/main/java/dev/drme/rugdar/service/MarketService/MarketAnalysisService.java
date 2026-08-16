package dev.drme.rugdar.service.MarketService;

import dev.drme.rugdar.dto.Analysis;
import dev.drme.rugdar.service.IdService;
import dev.drme.rugdar.utils.Log;
import dev.drme.rugdar.ws.MarketDataWsHandler;
import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MarketAnalysisService {

    private static final Logger log = Log.get(MarketAnalysisService.class);
    private final boolean analysisEnabled;
    private final String systemPrompt;

    private final ChatClient chatClient;
    private final SnapshotService snapshots;
    private final MarketDataWsHandler wsHandler;
    private final IdService ids;

    public MarketAnalysisService(
            @Value("${rugdar.ai.system-prompt}") String systemPrompt,
            @Value("${rugdar.ai.enabled}") boolean analysisEnabled,
            ChatClient.Builder chatClientBuilder,
            SnapshotService snapshots,
            MarketDataWsHandler wsHandler,
            IdService ids) {
        this.chatClient = chatClientBuilder.build();
        this.snapshots = snapshots;
        this.wsHandler = wsHandler;
        this.ids = ids;

        this.systemPrompt = systemPrompt;
        this.analysisEnabled = analysisEnabled;
    }

    @Scheduled(fixedRateString = "${rugdar.analysis.interval-seconds:30}000")
    public void analyzeCurrentMarket() {
        if (!analysisEnabled) {
            return;
        }

        try {
            String snapshot = snapshots.currentJson();
            log.info("Analyzing market snapshot ({} bytes)", snapshot.length());
            ChatResponse response = chatClient.prompt()
                    .system(this.systemPrompt)
                    .user(snapshot)
                    .call()
                    .chatResponse();
            String analysis = response.getResult().getOutput().getText();
            String model = response.getMetadata().getModel();
            Analysis record = new Analysis(
                    ids.next(),
                    model,
                    Instant.now(),
                    analysis
            );
            wsHandler.broadcast("analysis", record);
        } catch (Exception e) {
            log.warn("Market analysis failed", e);
        }
    }
}
