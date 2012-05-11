#! /usr/bin/env groovy
@Grab('redis.clients:jedis:2.0.0') 

def redis = new redis.clients.jedis.Jedis("localhost")

redis.flushDB()
redis.set("member:1:foo", "baz")
//redis.set("member:2:foo", "baz")
def member = [id: 1]
String[] keys = redis.keys("member:${member.id}:*")
if(keys) redis.del( keys )

redis.zadd "sset", 1, "one"
redis.zadd "sset", 4, "four"
redis.zadd "sset", 2, "two"
redis.zadd "sset", 3, "three"


def tuples = redis.zrangeWithScores("sset", -1, -1)

println tuples
println "done"
