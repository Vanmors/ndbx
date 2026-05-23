#!/bin/bash
set -e

CQLSH="cqlsh cassandra-test ${CASSANDRA_PORT}"

echo "Waiting for Cassandra to be ready..."
until $CQLSH -e "DESCRIBE KEYSPACES" > /dev/null 2>&1; do
  sleep 2
done
echo "Cassandra is ready."

echo "Creating keyspace and schema..."
$CQLSH <<EOF
CREATE KEYSPACE IF NOT EXISTS ${CASSANDRA_KEYSPACE}
  WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS ${CASSANDRA_KEYSPACE}.event_reactions (
  event_id text,
  created_by text,
  like_value tinyint,
  created_at timestamp,
  PRIMARY KEY (event_id, created_by)
);

CREATE INDEX IF NOT EXISTS ON ${CASSANDRA_KEYSPACE}.event_reactions (created_by);
CREATE INDEX IF NOT EXISTS ON ${CASSANDRA_KEYSPACE}.event_reactions (like_value);
EOF

echo "Cassandra schema initialized successfully."
