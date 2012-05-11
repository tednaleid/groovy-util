def redisService

redisService.foo = "bar"   
assert "bar" == redisService.foo   

redisService.sadd("months", "february")
assert true == redisService.sismember("months", "february")
