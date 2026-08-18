package dev.drme.rugdar.repository.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import dev.drme.rugdar.dto.Analysis;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;

class AnalysisRepositoryTest {

    private MongoTemplate mongoTemplate;
    private AnalysisRepository repository;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        repository = new AnalysisRepository(mongoTemplate);
    }

    @Test
    void saveInsertsDocumentWithAllFields() {
        Analysis analysis = new Analysis(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "deepseek/x", Instant.ofEpochMilli(1_700_000_000_000L), "bullish");

        repository.save(analysis);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(mongoTemplate).save(captor.capture(), eq("analyses"));
        Document doc = captor.getValue();
        assertThat(doc.get("_id")).isEqualTo(analysis.aid());
        assertThat(doc.getString("model")).isEqualTo("deepseek/x");
        assertThat(doc.getString("message")).isEqualTo("bullish");
        assertThat(doc.getDate("timestamp")).isEqualTo(Date.from(analysis.timestamp()));
    }

    @Test
    void findLatestSortsByTimestampDescAndLimits() {
        Document fresh = new Document()
                .append("_id", UUID.randomUUID())
                .append("model", "model-b")
                .append("timestamp", Date.from(Instant.ofEpochSecond(2_000)))
                .append("message", "fresh");
        Document old = new Document()
                .append("_id", UUID.randomUUID())
                .append("model", "model-a")
                .append("timestamp", Date.from(Instant.ofEpochSecond(1_000)))
                .append("message", "old");

        when(mongoTemplate.find(any(), eq(Document.class), eq("analyses")))
                .thenReturn(List.of(fresh, old));

        List<Analysis> result = repository.findLatest(20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).model()).isEqualTo("model-b");
        assertThat(result.get(0).message()).isEqualTo("fresh");
        assertThat(result.get(1).model()).isEqualTo("model-a");
    }

    @Test
    void findLatestReturnsEmptyWhenNoDocuments() {
        when(mongoTemplate.find(any(), eq(Document.class), eq("analyses")))
                .thenReturn(List.of());

        assertThat(repository.findLatest(20)).isEmpty();
    }
}
