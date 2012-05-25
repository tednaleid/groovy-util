#!/usr/bin/env groovy
@Grab('redis.clients:jedis:2.1.0')
 
import redis.clients.jedis.*

redis = new Jedis("localhost")
pipeline = new Jedis("localhost").pipelined()
random = new java.util.Random(13)
currentTime = System.currentTimeMillis()

def loadFileToSet(filePath, key, Closure transform = { it.toLowerCase() }) {
    new File(filePath).eachLine { line ->
        pipeline.sadd(key, transform(line))
    }
    pipeline.sync()
    println "Loaded ${redis.scard(key)} $key"
}

def getRandomUri() {
    redis.srandmember("uris")
}

String getRandomKey() {
    def key = [randomUri, random.nextInt(100000)]
    return key.join("/")
}

String getRandomTimestamp() {
    (currentTime - random.nextInt(100000)).toString()
}

redis.flushDB()

if (!redis.exists("uris")) loadFileToSet("./uniq_subjectids.txt", "uris")


void insertValue() {
    def key = randomKey
    //pipeline.set(key, randomTimestamp)
    pipeline.zadd("bigzset", new Double(randomTimestamp), key)
}

for (i in 1..10000000) {
    insertValue()
    if (i % 10000 == 0) {
        pipeline.sync()
        println "added $i"
    }
}

pipeline.sync()
