#!/usr/bin/env bash
set -euo pipefail

CFG_PORT="${MONGO_CFG_PORT:?MONGO_CFG_PORT is required}"
SHARD1A_PORT="${MONGO_SHARD1A_PORT:?MONGO_SHARD1A_PORT is required}"
SHARD1B_PORT="${MONGO_SHARD1B_PORT:?MONGO_SHARD1B_PORT is required}"
SHARD1C_PORT="${MONGO_SHARD1C_PORT:?MONGO_SHARD1C_PORT is required}"
SHARD2A_PORT="${MONGO_SHARD2A_PORT:?MONGO_SHARD2A_PORT is required}"
SHARD2B_PORT="${MONGO_SHARD2B_PORT:?MONGO_SHARD2B_PORT is required}"
SHARD2C_PORT="${MONGO_SHARD2C_PORT:?MONGO_SHARD2C_PORT is required}"

wait_for_mongo() {
  local host="$1"
  local port="$2"

  until mongosh --host "$host" --port "$port" --quiet --eval "db.adminCommand({ ping: 1 }).ok" >/dev/null 2>&1; do
    sleep 2
  done
}

wait_for_primary() {
  local host="$1"
  local port="$2"

  until [ "$(mongosh --host "$host" --port "$port" --quiet --eval "
    try {
      const status = rs.status();
      const hasPrimary = Array.isArray(status.members) && status.members.some((member) => member.stateStr === 'PRIMARY');
      print(hasPrimary ? '1' : '0');
    } catch (e) {
      print('0');
    }
  ")" = "1" ]; do
    sleep 2
  done
}

init_replica_set() {
  local host="$1"
  local port="$2"
  local config="$3"

  mongosh --host "$host" --port "$port" --quiet --eval "
    try {
      const status = rs.status();
      if (status.ok === 1) {
        quit(0);
      }
    } catch (e) {}
    rs.initiate($config);
  "
}

wait_for_mongo cfg1 "$CFG_PORT"
wait_for_mongo shard1a "$SHARD1A_PORT"
wait_for_mongo shard1b "$SHARD1B_PORT"
wait_for_mongo shard1c "$SHARD1C_PORT"
wait_for_mongo shard2a "$SHARD2A_PORT"
wait_for_mongo shard2b "$SHARD2B_PORT"
wait_for_mongo shard2c "$SHARD2C_PORT"

init_replica_set cfg1 "$CFG_PORT" "{ _id: 'cfgReplSet', configsvr: true, members: [{ _id: 0, host: 'cfg1:${CFG_PORT}' }] }"
init_replica_set shard1a "$SHARD1A_PORT" "{ _id: 'shard1', members: [{ _id: 0, host: 'shard1a:${SHARD1A_PORT}' }, { _id: 1, host: 'shard1b:${SHARD1B_PORT}' }, { _id: 2, host: 'shard1c:${SHARD1C_PORT}' }] }"
init_replica_set shard2a "$SHARD2A_PORT" "{ _id: 'shard2', members: [{ _id: 0, host: 'shard2a:${SHARD2A_PORT}' }, { _id: 1, host: 'shard2b:${SHARD2B_PORT}' }, { _id: 2, host: 'shard2c:${SHARD2C_PORT}' }] }"

wait_for_primary cfg1 "$CFG_PORT"
wait_for_primary shard1a "$SHARD1A_PORT"
wait_for_primary shard2a "$SHARD2A_PORT"

wait_for_mongo mongos "${MONGODB_PORT}"

mongosh --host mongos --port "${MONGODB_PORT}" --quiet <<'JS'
const dbName = process.env.MONGODB_DATABASE || "eventhub";
const user = process.env.MONGODB_USER || "";
const password = process.env.MONGODB_PASSWORD || "";

const shard1a = process.env.MONGO_SHARD1A_PORT;
const shard1b = process.env.MONGO_SHARD1B_PORT;
const shard1c = process.env.MONGO_SHARD1C_PORT;
const shard2a = process.env.MONGO_SHARD2A_PORT;
const shard2b = process.env.MONGO_SHARD2B_PORT;
const shard2c = process.env.MONGO_SHARD2C_PORT;

const existingShards = db.adminCommand({ listShards: 1 }).shards.map((shard) => shard._id);
if (!existingShards.includes("shard1")) {
  sh.addShard("shard1/shard1a:" + shard1a + ",shard1b:" + shard1b + ",shard1c:" + shard1c);
}
if (!existingShards.includes("shard2")) {
  sh.addShard("shard2/shard2a:" + shard2a + ",shard2b:" + shard2b + ",shard2c:" + shard2c);
}

sh.enableSharding(dbName);

try {
  sh.shardCollection(
    `${dbName}.events`,
    { created_by: "hashed" },
    false,
    { numInitialChunks: 8 }
  );
} catch (e) {
  if (!String(e).includes("already")) {
    throw e;
  }
}

if (user !== "" && password !== "") {
  const appDb = db.getSiblingDB(dbName);
  if (!appDb.getUser(user)) {
    appDb.createUser({
      user,
      pwd: password,
      roles: [{ role: "readWrite", db: dbName }]
    });
  }
}
JS