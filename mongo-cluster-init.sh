#!/usr/bin/env bash
set -euo pipefail

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

wait_for_mongo cfg1 27100
wait_for_mongo shard1a 27101
wait_for_mongo shard1b 27102
wait_for_mongo shard1c 27103
wait_for_mongo shard2a 27104
wait_for_mongo shard2b 27105
wait_for_mongo shard2c 27106

init_replica_set cfg1 27100 '{ _id: "cfgReplSet", configsvr: true, members: [{ _id: 0, host: "cfg1:27100" }] }'
init_replica_set shard1a 27101 '{ _id: "shard1", members: [{ _id: 0, host: "shard1a:27101" }, { _id: 1, host: "shard1b:27102" }, { _id: 2, host: "shard1c:27103" }] }'
init_replica_set shard2a 27104 '{ _id: "shard2", members: [{ _id: 0, host: "shard2a:27104" }, { _id: 1, host: "shard2b:27105" }, { _id: 2, host: "shard2c:27106" }] }'

wait_for_primary cfg1 27100
wait_for_primary shard1a 27101
wait_for_primary shard2a 27104

wait_for_mongo mongos "${MONGODB_PORT}"

mongosh --host mongos --port "${MONGODB_PORT}" --quiet <<'JS'
const dbName = process.env.MONGODB_DATABASE || "eventhub";
const user = process.env.MONGODB_USER || "";
const password = process.env.MONGODB_PASSWORD || "";

const existingShards = db.adminCommand({ listShards: 1 }).shards.map((shard) => shard._id);
if (!existingShards.includes("shard1")) {
  sh.addShard("shard1/shard1a:27101,shard1b:27102,shard1c:27103");
}
if (!existingShards.includes("shard2")) {
  sh.addShard("shard2/shard2a:27104,shard2b:27105,shard2c:27106");
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