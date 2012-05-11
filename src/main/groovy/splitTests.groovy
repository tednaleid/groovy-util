#! /usr/bin/env groovy

// usage: bin/splitTests.groovy [optional grails commands to run first]
// ex: bin/splitTests.groovy clean package
// runs grails clean, grails package, and then grails test-app for each of unit and integration

Map exitStatuses = [:].asSynchronized()

def currentDir() { 'pwd'.execute().text.trim() }

def getGrailsWithPropertyArgs(group) {
	def testDir = "${currentDir()}/test-$group"
	[
        "grails",
	    "-Dgrails.work.dir=${testDir}",
	    "-Dgrails.project.class.dir=${testDir}/classes",
	    "-Dgrails.project.test.class.dir=${testDir}/test-classes",
	    "-Dgrails.project.test.reports.dir=${testDir}/test-reports"
	].join(" ")
}

def getGrailsCommand(testGroup, command) {
    return "${getGrailsWithPropertyArgs(testGroup)} $command"
}

synchronized out(group, message) {
    println("${group.padLeft(12, ' ')}: $message")
}

def runCommand(testGroup, command) {
		out testGroup, command
		return command.execute().with { proc ->
			proc.in.eachLine { line -> out testGroup, line }
			proc.waitFor()
			proc.exitValue()
		}
}

def runCommandsFirst = args ?: []

[
        integration: 'integration:',
        unit: 'unit:'
].collect { testGroup, testArgs ->
	Thread.start {
        if (testGroup == 'integration') {
            out testGroup, "sleeping for 5 seconds to let the other side get some work done first"
            sleep(5000)
        }
        for (String command : runCommandsFirst) {
           exitStatuses[testGroup] = runCommand(testGroup, getGrailsCommand(testGroup, command))
           if (exitStatuses[testGroup]) break
        }
        if (!exitStatuses[testGroup]) { 
            exitStatuses[testGroup] = runCommand(testGroup, getGrailsCommand(testGroup, "test-app $testArgs"))
        }
		out testGroup, "exit value: ${exitStatuses[testGroup]}"
	}
}.each { it.join() }

def failingGroups = exitStatuses.findAll { it.value }

if (!failingGroups) {
	println "All tests were successful!"
} else {
	failingGroups.each { failingGroup, exitStatus ->
		out(failingGroup, "WARNING: '$failingGroup' exit status was $exitStatus")
	}
	System.exit(-1)
}
