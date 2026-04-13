package com.vanmors.ndbx.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${MONGODB_HOST}")
    private String host;

    @Value("${MONGODB_PORT}")
    private int port;

    @Value("${MONGODB_DATABASE}")
    private String database;

    @Value("${MONGODB_USER}")
    private String username;

    @Value("${MONGODB_PASSWORD}")
    private String password;

    @Bean
    public MongoClient mongoClient() {
        final StringBuilder uri = new StringBuilder("mongodb://");

        if (!username.isEmpty() && !password.isEmpty()) {
            uri.append(username).append(":").append(password).append("@");
        }

        uri.append(host).append(":").append(port).append("/").append(database);

        if (!username.isEmpty()) {
            uri.append("?authSource=admin");
        }

        final String connectionString = uri.toString();
        log.info("MongoDB connection string: {}", connectionString.replace(password, "****"));

        return MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build());
    }

    @Bean
    public MongoTemplate mongoTemplate(final MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, database);
    }

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public boolean autoIndexCreation() {
        return true;
    }
}