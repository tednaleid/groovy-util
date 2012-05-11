#!/usr/bin/env groovy
@Grab('redis.clients:jedis:2.0.0')
import redis.clients.jedis.*

redis = new Jedis("localhost")

println "Joining the welcome-wagon!"

while (true) {
    def name = redis.blpop(0, "welcome-wagon")[1]
    println "Welcome ${name}!"
}
