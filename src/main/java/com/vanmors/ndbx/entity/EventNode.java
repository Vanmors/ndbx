package com.vanmors.ndbx.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Event")
public class EventNode {

    @Id
    @GeneratedValue
    private Long neo4jId;

    @Property("id")
    private String mongoId;

    @Property("title")
    private String title;

    public EventNode() {
    }

    public EventNode(String mongoId, String title) {
        this.mongoId = mongoId;
        this.title = title;
    }

    public Long getNeo4jId() {
        return neo4jId;
    }

    public String getMongoId() {
        return mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
