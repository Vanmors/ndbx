package com.vanmors.ndbx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.cassandra.autoconfigure.CassandraAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;


@SpringBootApplication
@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class, CassandraAutoConfiguration.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
