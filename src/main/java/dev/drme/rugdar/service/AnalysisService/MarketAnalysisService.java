package dev.drme.rugdar.service.AnalysisService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.drme.rugdar.dto.Analysis;
import dev.drme.rugdar.dto.MarketSummary;
import dev.drme.rugdar.repository.analysis.AnalysisRepository;
import dev.drme.rugdar.repository.market.MarketDataRepository;
import dev.drme.rugdar.service.IdService;
import dev.drme.rugdar.service.MarketService.MarketDataService;
import dev.drme.rugdar.utils.JsonUtils;
import dev.drme.rugdar.utils.Log;
import dev.drme.rugdar.ws.MarketAnalysisHandler;
import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(AnalysisRepository.class)
public class MarketAnalysisService {

    private static final Logger log = Log.get(MarketAnalysisService.class);

    private final boolean analysisEnabled;
    private final String systemPrompt;
    private final int historyDays;

    private final ChatClient chatClient;
    private final MarketAnalysisHandler analysisHandler;
    private final IdService ids;
    private final AnalysisRepository analysisRepository;
    private final MarketDataRepository marketDataRepository;
    private final MarketDataService marketDataService;

    public MarketAnalysisService(
            @Value("${rugdar.ai.system-prompt}") String systemPrompt,
            @Value("${rugdar.ai.enabled}") boolean analysisEnabled,
            @Value("${rugdar.analysis.history-days:7}") int historyDays,
            ChatClient.Builder chatClientBuilder,
            MarketAnalysisHandler analysisHandler,
            IdService ids,
            AnalysisRepository analysisRepository,
            MarketDataRepository marketDataRepository,
            MarketDataService marketDataService) {
        this.chatClient = chatClientBuilder.build();
        this.analysisHandler = analysisHandler;
        this.ids = ids;
        this.analysisRepository = analysisRepository;
        this.marketDataRepository = marketDataRepository;
        this.marketDataService = marketDataService;

        this.systemPrompt = systemPrompt;
        this.analysisEnabled = analysisEnabled;
        this.historyDays = historyDays;
    }

    @Scheduled(fixedRateString = "${rugdar.analysis.interval-minutes:60}0000")
    public void analyzeCurrentMarket() {
        if (!analysisEnabled) {
            return;
        }

        try {
            List<MarketSummary> summary = marketDataRepository.summarize(historyDays);
            if (summary.isEmpty()) {
                log.warn("No market data in DB, skipping analysis");
                return;
            }
            String prompt = JsonUtils.serialize(withIndicators(summary));
            log.info("Analyzing market history ({} symbols, {} days)", summary.size(), historyDays);
            ChatResponse response = chatClient.prompt()
                    .system(this.systemPrompt)
                    .user(prompt)
                    .call()
                    .chatResponse();
            if (response == null) {
                log.warn("Analysis failed: no response");
                return;
            }
            Generation result = response.getResult();
            if (result == null) {
                log.warn("Analysis failed: no generation");
                return;
            }
            String analysis = result.getOutput().getText();
            String model = response.getMetadata().getModel();
            Analysis record = new Analysis(ids.next(), model, Instant.now(), analysis);
            analysisHandler.broadcast("analysis", record);
            analysisRepository.save(record);
        } catch (Exception e) {
            log.warn("Market analysis failed", e);
        }
    }

    private List<Map<String, Object>> withIndicators(List<MarketSummary> summary) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MarketSummary s : summary) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("exchange", s.exchange());
            row.put("symbol", s.symbol());
            row.put("lastPrice", s.lastPrice());
            row.put("minPrice", s.minPrice());
            row.put("maxPrice", s.maxPrice());
            row.put("avgPrice", s.avgPrice());
            row.put("totalVolume", s.totalVolume());
            row.put("totalTurnover", s.totalTurnover());
            row.put("samples", s.samples());
            marketDataService.indicators(s.exchange(), s.symbol()).ifPresent(ind -> {
                row.put("sma20", ind.sma());
                row.put("ema20", ind.ema());
            });
            rows.add(row);
        }
        return rows;
    }
}
