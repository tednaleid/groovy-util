#!/usr/bin/env groovy
@Grab('redis.clients:jedis:2.0.0')
import redis.clients.jedis.*

redis = new Jedis("localhost")

args.each { redis.lpush("welcome-wagon", it) }

