#!/usr/bin/env groovy
@Grab('redis.clients:jedis:2.0.0')
 
import redis.clients.jedis.*

redis = new Jedis("localhost")
pipeline = new Jedis("localhost").pipelined()
random = new java.util.Random(13)

def loadFileToSet(filePath, key, Closure transform = { it.toLowerCase() }) {
    new File(filePath).eachLine { line ->
        pipeline.sadd(key, transform(line))
    }
    pipeline.sync()
    println "Loaded ${redis.scard(key)} $key"
}

def getRandomWord() {
    redis.srandmember("words")
}

def getRandomKey() {
    def key = ["member", random.nextInt(100000)]
    for (i in 1..4) {
        def word = randomWord
        key << word 
    }
    return key.join(":")
}

//redis.flushDB()

// dictionary words
if (!redis.exists("words")) loadFileToSet("/usr/share/dict/words", "words")

for (i in 1..1000000) {
    def key = randomKey 
    pipeline.set(key, randomWord)
    if (i % 1000 == 0) {
        pipeline.sync()
        println "added $i"
    }
}

pipeline.sync()



