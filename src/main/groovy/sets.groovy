#!/usr/bin/env groovy
@Grab('redis.clients:jedis:2.0.0')
 
import redis.clients.jedis.*

redis = new Jedis("localhost")
redis.flushDB()

def tuples = [
        ['A', 'B'],
        ['B', 'C'],
        ['D', 'G'],
        ['H', 'I'],
        ['D', 'B']
]

def keyFor(element) {
    "entity:$element"
}

def setKeyFor(element) {
    "set:$element"
}

def createSetFor(element) {
    def setKey = setKeyFor(element)
    redis.sadd(setKeyFor(element), element)
    redis.set(keyFor(element), setKey)
}

def addTuple(origTuple) {
    def tuple = origTuple.clone()

    tuple.sort() // sort the tuple

    // determine which elements already exist and which ones need to be joined together
    def (newElements, existingElements) = tuple.split { element -> !redis.exists(keyFor(element)) }
    
    // the last existing element is our master, otherwise if all new, make the first one master
    def masterElement
    if (existingElements) {
        masterElement = existingElements.pop()    
    } else {
        masterElement = newElements.pop()
        createSetFor(masterElement)
    }

    def targetSetKey = setKeyFor(masterElement)

    // now insert all entities in the tuple that don't exist yet into the set
    for (element in newElements) {
        redis.set(keyFor(element), targetSetKey)
        redis.sadd(targetSetKey, element)
    }
    
    def unionSetKeys = []
    for (element in existingElements) {
        // point all the existing keys at the target key that we're unioning things into
        redis.set(keyFor(element), targetSetKey)
        def elementSetKey = setKeyFor(element)
        if (elementSetKey != targetSetKey) unionSetKeys << elementSetKey
    }

    // now union in all the old tuples that don't already point to our set key
    for (unionSetKey in unionSetKeys) {
        // TODO: would be nice to call varargs method here instead of looping
        redis.sunionstore(targetSetKey, targetSetKey, unionSetKey)
    }

    printState(tuple)
}

def printState(tuple) {
    println "\n\nafter tuple: $tuple"

    redis.keys("entity:*").sort().each { key ->
        println "$key -> ${redis.get(key)}"
    }

    redis.keys("set:*").sort().each { setKey ->
        println "$setKey -> ${redis.smembers(setKey)}"
    }
    

}


for(tuple in tuples) {
    addTuple(tuple)
}

                    
