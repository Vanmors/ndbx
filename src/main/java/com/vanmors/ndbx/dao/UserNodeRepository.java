package com.vanmors.ndbx.dao;

import com.vanmors.ndbx.entity.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {

    @Query("MATCH (u:User {id: $mongoId}) RETURN u")
    Optional<UserNode> findByMongoId(@Param("mongoId") String mongoId);

    @Query("MERGE (u:User {id: $mongoId}) RETURN u")
    UserNode mergeByMongoId(@Param("mongoId") String mongoId);
}
