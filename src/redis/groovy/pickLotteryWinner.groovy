#!/usr/bin/env groovy
@Grab('redis.clients:jedis:2.0.0')

redis = new redis.clients.jedis.Jedis("localhost")

def printPrizeWinner(prize, winner) { println "$winner wins $prize" }

def cli = new CliBuilder(usage: "pickLotteryWinner.groovy [-lf] <prize>")

cli.with {
    l longOpt: 'list', args: 0, argName: 'list', 'List all winners'
    f longOpt: 'flushdb', args: 0, argName: 'flushdb', 'Flush all results'
}

def options = cli.parse(args)

if (options.'list') {
    def prizeWinners = redis.hgetAll("prize-winners")
    println "Prize winners so far:"
    prizeWinners.each { prize, winner -> printPrizeWinner(prize, winner) }
    System.exit(0)
}
   
if (options.'flushdb') {
    redis.flushDB()
    println "Flushing Redis..."
    System.exit(0)
}

if (!options.arguments()) {
    println "Please enter a prize that you'd like to pick a winner for"
    cli.usage()
    System.exit(-1)
} else {
    prize = options.arguments().join(" ")
}

// load up the set of conference attendees if it's not already there, one per line
if (!redis.exists("conference-attendees")) {
    new File("./conference-attendees.txt").eachLine { redis.sadd("conference-attendees", it) }
}

// pop a random winner out of the list of attendees
def winner = redis.spop("conference-attendees")

// add them to a hashmap that maps a prize to a winner
redis.hset("prize-winners", prize, winner)

printPrizeWinner(prize, winner)
