package org.rugdar.service.MarketService;

import java.util.List;

import org.rugdar.dto.Ticker;
import org.rugdar.utils.JsonUtils;
import org.springframework.stereotype.Service;

@Service
public class SnapshotService {

    private final MarketDataService marketData;

    public SnapshotService(MarketDataService marketData) {
        this.marketData = marketData;
    }

    public String currentJson() {
        List<Ticker> all = List.copyOf(marketData.allLatest().values());
        return JsonUtils.serialize(all);
    }
}
