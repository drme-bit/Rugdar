package dev.drme.rugdar.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("'${spring.mongodb.uri:}' != ''")
class MongoConfig {

    @Bean
    MongoClient mongoClient(@Value("${spring.mongodb.uri}") String uri) {
        var settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .build();
        return MongoClients.create(settings);
    }
}
