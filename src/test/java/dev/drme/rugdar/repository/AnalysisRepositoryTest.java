package dev.drme.rugdar.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.mongodb.ConnectionString;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import dev.drme.rugdar.dto.Analysis;
import dev.drme.rugdar.repository.analysis.AnalysisRepository;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;

class AnalysisRepositoryTest {

    private MongoCollection<Document> collection;
    private AnalysisRepository repository;

    @BeforeEach
    void setUp() {
        MongoClient client = mock(MongoClient.class);
        MongoDatabase database = mock(MongoDatabase.class);
        MongoConnectionDetails details = mock(MongoConnectionDetails.class);

        when(details.getConnectionString())
                .thenReturn(new ConnectionString("mongodb://localhost/Cluster0"));
        when(client.getDatabase("Cluster0")).thenReturn(database);
        collection = mock(MongoCollection.class);
        when(database.getCollection("analyses")).thenReturn(collection);

        repository = new AnalysisRepository(client, details);
    }

    @Test
    void saveInsertsDocumentWithAllFields() {
        Analysis analysis = new Analysis(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "deepseek/x", Instant.ofEpochMilli(1_700_000_000_000L), "bullish");

        repository.save(analysis);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(collection).insertOne(captor.capture());
        Document doc = captor.getValue();
        assertThat(doc.get("_id")).isEqualTo(analysis.aid());
        assertThat(doc.getString("model")).isEqualTo("deepseek/x");
        assertThat(doc.getString("message")).isEqualTo("bullish");
        assertThat(doc.getDate("timestamp")).isEqualTo(Date.from(analysis.timestamp()));
    }

    private MongoCursor<Document> cursor(List<Document> docs) {
        java.util.Iterator<Document> it = docs.iterator();
        return new MongoCursor<>() {
            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Document next() {
                return it.next();
            }

            @Override
            public int available() {
                return 0;
            }

            @Override
            public Document tryNext() {
                return it.hasNext() ? it.next() : null;
            }

            @Override
            public com.mongodb.ServerCursor getServerCursor() {
                return null;
            }

            @Override
            public com.mongodb.ServerAddress getServerAddress() {
                return null;
            }
        };
    }

    @Test
    void findLatestSortsByTimestampDescAndLimits() {
        FindIterable<Document> iterable = mock(FindIterable.class);
        when(collection.find()).thenReturn(iterable);
        when(iterable.sort(any())).thenReturn(iterable);
        when(iterable.limit(20)).thenReturn(iterable);

        Document old = new Document()
                .append("_id", UUID.randomUUID())
                .append("model", "model-a")
                .append("timestamp", Date.from(Instant.ofEpochSecond(1_000)))
                .append("message", "old");
        Document fresh = new Document()
                .append("_id", UUID.randomUUID())
                .append("model", "model-b")
                .append("timestamp", Date.from(Instant.ofEpochSecond(2_000)))
                .append("message", "fresh");
        when(iterable.iterator()).thenReturn(cursor(List.of(fresh, old)));

        List<Analysis> result = repository.findLatest(20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).model()).isEqualTo("model-b");
        assertThat(result.get(0).message()).isEqualTo("fresh");
        assertThat(result.get(1).model()).isEqualTo("model-a");

        verify(iterable).sort(argThat(bson ->
                bson.toBsonDocument(Document.class,
                        com.mongodb.MongoClientSettings.getDefaultCodecRegistry())
                        .containsKey("timestamp")));
        verify(iterable).limit(20);
    }

    @Test
    void findLatestReturnsEmptyWhenNoDocuments() {
        FindIterable<Document> iterable = mock(FindIterable.class);
        when(collection.find()).thenReturn(iterable);
        when(iterable.sort(any())).thenReturn(iterable);
        when(iterable.limit(20)).thenReturn(iterable);
        when(iterable.iterator()).thenReturn(cursor(List.of()));

        assertThat(repository.findLatest(20)).isEmpty();
    }
}
