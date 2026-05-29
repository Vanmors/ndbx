package com.vanmors.ndbx.dao;

import com.vanmors.ndbx.entity.EventNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventNodeRepository extends Neo4jRepository<EventNode, Long> {

    @Query("MATCH (e:Event {id: $mongoId}) RETURN e")
    Optional<EventNode> findByMongoId(@Param("mongoId") String mongoId);

    @Query("MERGE (e:Event {id: $mongoId}) ON CREATE SET e.title = $title RETURN e")
    EventNode mergeByMongoId(@Param("mongoId") String mongoId, @Param("title") String title);

    @Query("MATCH (u:User {id: $userId})-[:LIKED]->(e:Event) RETURN e.id")
    List<String> findLikedEventIds(@Param("userId") String userId);

    @Query("""
            MATCH (u:User {id: $userId})-[:LIKED]->(e:Event)<-[:LIKED]-(other:User)-[:LIKED]->(rec:Event)
            WHERE NOT (u)-[:LIKED]->(rec)
            WITH DISTINCT rec
            MATCH (liker:User)-[:LIKED]->(rec)
            RETURN rec.id AS eventId, COUNT(liker) AS score
            ORDER BY score DESC
            """)
    List<RecommendationResult> findRecommendations(@Param("userId") String userId);

    @Query("""
            MATCH (u:User {id: $userId}), (e:Event {id: $eventId})
            MERGE (u)-[:LIKED]->(e)
            """)
    void createLikedRelationship(@Param("userId") String userId, @Param("eventId") String eventId);

    record RecommendationResult(String eventId, Long score) {
        public String getEventId() { return eventId; }
        public Long getScore() { return score; }
    }
}
