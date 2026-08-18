package dev.drme.rugdar.repository.market;

import dev.drme.rugdar.dto.MarketSummary;
import dev.drme.rugdar.dto.Ticker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;


@Repository
public class MarketDataRepository {
    private static final String TABLE = "market_data";

    private final JdbcTemplate jdbcTemplate;

    public MarketDataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveAll(List<Ticker> tickers) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO market_data (id, exchange, symbol, high, low, last_price, ts, turnover, open, volume) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tickers.stream()
                        .map(t -> new Object[]{
                                t.id(), t.exchange(), t.symbol(),
                                t.high(), t.low(), t.lastPrice(),
                                Timestamp.from(t.timestamp()), t.turnover(),
                                t.open(), t.volume()})
                        .toList());
    }

    public int deleteOlderThan(int days) {
        return jdbcTemplate.update(
                "DELETE FROM " + TABLE + " WHERE ts < now() - make_interval(days => ?)",
                days);
    }

    public List<MarketSummary> summarize(int days) {
        return jdbcTemplate.query(
                "SELECT exchange, symbol, "
                        + "(array_agg(last_price ORDER BY ts DESC))[1] AS last_price, "
                        + "MIN(last_price) AS min_price, "
                        + "MAX(last_price) AS max_price, "
                        + "AVG(last_price) AS avg_price, "
                        + "SUM(volume) AS volume, "
                        + "SUM(turnover) AS turnover, "
                        + "COUNT(*) AS samples "
                        + "FROM " + TABLE + " "
                        + "WHERE ts >= now() - make_interval(days => ?) "
                        + "GROUP BY exchange, symbol "
                        + "ORDER BY exchange, symbol",
                (rs, _) -> new MarketSummary(
                        rs.getString("exchange"),
                        rs.getString("symbol"),
                        rs.getBigDecimal("last_price"),
                        rs.getBigDecimal("min_price"),
                        rs.getBigDecimal("max_price"),
                        rs.getBigDecimal("avg_price"),
                        rs.getBigDecimal("volume"),
                        rs.getBigDecimal("turnover"),
                        rs.getInt("samples")),
                days);
    }
}
