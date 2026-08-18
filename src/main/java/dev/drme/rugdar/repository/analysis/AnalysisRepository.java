package dev.drme.rugdar.repository.analysis;


import dev.drme.rugdar.dto.Analysis;
import org.bson.Document;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class AnalysisRepository {

    private static final String COLLECTION = "analyses";

    private final MongoTemplate mongoTemplate;

    public AnalysisRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void save(Analysis analysis) {
        Document document = new Document()
                .append("_id", analysis.aid())
                .append("model", analysis.model())
                .append("timestamp", Date.from(analysis.timestamp()))
                .append("message", analysis.message());
        
        mongoTemplate.save(document, COLLECTION);
    }

    public List<Analysis> findLatest(int limit) {
        Query query = new Query()
                .with(Sort.by(Sort.Direction.DESC, "timestamp"))
                .limit(limit);

        return mongoTemplate.find(query, Document.class, COLLECTION)
                .stream()
                .map(doc -> new Analysis(
                        doc.get("_id", UUID.class),
                        doc.getString("model"),
                        doc.getDate("timestamp").toInstant(),
                        doc.getString("message")))
                .toList();
    }
}
