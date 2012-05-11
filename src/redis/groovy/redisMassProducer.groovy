#!/usr/bin/env groovy
@Grab('redis.clients:jedis:2.0.0')
import redis.clients.jedis.*

redis = new Jedis("localhost")

if (!redis.exists("female-names")) {
    new File("./female-names.txt").eachLine { redis.sadd("female-names", it) }
}

for (i in 1..100000) { 
    redis.lpush("welcome-wagon", redis.srandmember("female-names"))
    if (i % 1000 == 0) println "Adding $i"
}

