package dev.drme.rugdar.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

import dev.drme.rugdar.dto.Analysis;
import dev.drme.rugdar.repository.analysis.AnalysisRepository;
import dev.drme.rugdar.utils.Log;
import org.slf4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class MarketAnalysisHandler extends TopicWebSocketHandler {

    private static final Logger log = Log.get(MarketAnalysisHandler.class);

    private static final int ANALYSIS_HISTORY_LIMIT = 20;

    private final AnalysisRepository analysisRepository;

    public MarketAnalysisHandler(ObjectMapper mapper, AnalysisRepository analysisRepository) {
        super(mapper);
        this.analysisRepository = analysisRepository;
    }

    @EventListener
    public void onAnalysis(Analysis analysis) {
        broadcast("analysis", analysis);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        super.afterConnectionEstablished(session);
        sendAnalysisHistory(session);
    }

    private void sendAnalysisHistory(WebSocketSession session) {
        try {
            List<Analysis> history = analysisRepository.findLatest(ANALYSIS_HISTORY_LIMIT);
            send(session, Map.of(
                    "v", 1,
                    "type", "analysis_history",
                    "seq", nextSeq(),
                    "data", history));
        } catch (Exception e) {
            log.warn("Failed to load analysis history", e);
        }
    }
}
