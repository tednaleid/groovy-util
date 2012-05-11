#!/usr/bin/env groovy
@Grab('redis.clients:jedis:2.0.0')
 
import redis.clients.jedis.*

redis = new Jedis("localhost")
pipeline = redis.pipelined()

def loadFileToSet(filePath, key, Closure transform = { it.toLowerCase() }) {
    new File(filePath).eachLine { line ->
        pipeline.sadd(key, transform(line))
    }
    pipeline.sync()
    println "Loaded ${redis.scard(key)} $key"
}

redis.flushDB()

// dictionary words
loadFileToSet("/usr/share/dict/words", "words")

// reversed dictionary words
loadFileToSet("/usr/share/dict/words", "reversed-words") { it.reverse().toLowerCase() }

// female names
loadFileToSet("/Users/tnaleid/Documents/workspace/groovy-util/redis/female-names.txt", "female-names")


