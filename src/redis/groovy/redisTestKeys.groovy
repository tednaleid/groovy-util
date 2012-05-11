#! /usr/bin/env groovy
@Grab('redis.clients:jedis:2.0.0') 

def redis = new redis.clients.jedis.Jedis("localhost")


def withTiming(name, closure) {
    def start = System.currentTimeMillis()
    closure(start)
    def end = System.currentTimeMillis()
    println "Time for $name : " + (end - start) / 1000
}

def random = new java.util.Random(13)

def name = "redis keys search"
withTiming(name) { start -> 
    for (i in 1..1000) {
        // string prefix doesn't match even with the first character
        redis.keys("NOMATCH:" + random.nextInt(10000) + "*") // search for NOMATCH:12345*

        // string prefix matches for first 13 characters then doesn't match anything
        //redis.keys("member:" + random.nextInt(10000) + "NOMATCH*") // search for member:12345NOMATCH*

        // string prefix matches, suffix after wildcard doesn't match anything
        //redis.keys("*:" + random.nextInt(1000) + "*NOMATCH")       // search for member:1234*NOMATCH

        // infix string matches for 13 characters, then doesn't match anything
        //redis.keys("*:" + random.nextInt(10000) + "NOMATCH*")       // search for *:12345NOMATCH*
    
        if (i % 10 == 0) println "Time for $name, at $i : " + (System.currentTimeMillis() - start) / 1000
    }
}

