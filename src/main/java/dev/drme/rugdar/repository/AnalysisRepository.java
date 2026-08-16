package dev.drme.rugdar.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.drme.rugdar.dto.Analysis;
import org.bson.Document;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class AnalysisRepository {

    private static final String COLLECTION = "analyses";

    private final MongoCollection<Document> collection;

    public AnalysisRepository(MongoClient mongoClient, MongoConnectionDetails details) {
        MongoDatabase db = mongoClient.getDatabase(details.getConnectionString().getDatabase());
        this.collection = db.getCollection(COLLECTION);
    }

    public void save(Analysis analysis) {
        collection.insertOne(new Document()
                .append("_id", analysis.aid())
                .append("model", analysis.model())
                .append("timestamp", Date.from(analysis.timestamp()))
                .append("message", analysis.message()));
    }

    public List<Analysis> findLatest(int limit) {
        List<Analysis> result = new ArrayList<>();
        for (Document doc : collection.find()
                .sort(new Document("timestamp", -1))
                .limit(limit)) {
            result.add(new Analysis(
                    doc.get("_id", java.util.UUID.class),
                    doc.getString("model"),
                    doc.getDate("timestamp").toInstant(),
                    doc.getString("message")));
        }
        return result;
    }
}
