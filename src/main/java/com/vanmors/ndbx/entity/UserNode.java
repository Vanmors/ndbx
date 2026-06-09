package com.vanmors.ndbx.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("User")
public class UserNode {

    @Id
    @GeneratedValue
    private Long neo4jId;

    @Property("id")
    private String mongoId;

    public UserNode() {
    }

    public UserNode(String mongoId) {
        this.mongoId = mongoId;
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
}
