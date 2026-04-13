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
    @Override
    public MongoClient mongoClient() {
        final String uri = String.format(
                "mongodb://%s:%s@%s:%d/%s?authSource=%s&retryWrites=true",
                username,
                password,
                host,
                port,
                database,
                database
        );

        return MongoClients.create(uri);
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