#!/usr/bin/env groovy
// groovy version of ruby script from here: http://antirez.com/post/autocomplete-with-redis.html
@Grapes([
    @Grab('redis.clients:jedis:1.5.1'),
    @GrabConfig(systemClassLoader=true)
])
 
import redis.clients.jedis.*

class Autocomplete { 
    Jedis jedis = new Jedis("localhost")
    Integer rangeSize = 50  // try to get replies < MTU size

    def loadCompletionFile(String path) {
        if (jedis.exists("autocomplete")) return
        new File(path).eachLine { line ->
            if (!line.startsWith("#")) ac.addToAutocomplete(line)
        }
    }

    def addToAutocomplete(String string) {
        def full = "${string.trim()}*"
        for ( i in (0..<full.size())) {
            jedis.zadd("autocomplete", 0, full[0..i])
        }
    }

    def complete(String prefix, Integer maxResults) {
        Integer start = jedis.zrank("autocomplete", prefix)

        if (start == null) return []

        def results = []

        while (true) {
            def range = jedis.zrange("autocomplete", start, start + rangeSize - 1)
            if (!range) return results 
            start += rangeSize
            for (entry in range) {
                if (!entry.startsWith(prefix)) return results
                if (entry[-1..-1] == "*") results << entry[0..-2]
                if (results.size() >= maxResults) return results
            }
        }
    }
}

def ac = new Autocomplete()
ac.loadCompletionFile("./female-names.txt")
def prefix = (args ?: ["marc"])[0]  // default if there aren't any parameters passed in

ac.complete(prefix, 50).each { match ->
    println "match! -> $match"
}
