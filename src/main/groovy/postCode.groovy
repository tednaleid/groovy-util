#! /usr/bin/env groovy
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )
// prevent range scan for all groovy versions since we're using groovy, elimiate 30 sec pause in startup
@GrabExclude("org.codehaus.groovy:groovy")

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.Method.*
import java.net.URLDecoder
import org.apache.http.util.EntityUtils

class GrailsConsoleCodePoster {
    String baseUrl
    // Spring Security default url
    String authenticationPath = "j_spring_security_check"
    // Grails Console plugin default execute path
    String executePath = "console/execute"
    // Spring Security default failure url path
    String failedAuthenticationPath = "signin/authfail"
    String codeFilePath
    String username
    String password
    private HTTPBuilder httpBuilder

    GrailsConsoleCodePoster(args) {
        parseArgs(args)
    }

    HTTPBuilder getHttp() {
        def url = baseUrl.endsWith("/") ? baseUrl : "${baseUrl}/"
        if (!httpBuilder) httpBuilder = new HTTPBuilder(url)
        return httpBuilder
    }

    def postCodeAfterOptionalAuth() {
        if (!shouldAuthenticateFirst() || authenticate() ) {
            postCode()
        } else {
            println "ERROR: failed to authenticate properly, can't post code"
        }
    }        

    boolean shouldAuthenticateFirst() {
        return (username && password)
    }

    def postCode() {
        File codeFile = new File(codeFilePath)
        if (!codeFile.exists()) throw new Exception("Unable to find file $codeFilePath")

        http.request( POST ) {
            uri.path = executePath
            requestContentType = URLENC
            body = [captureStdout: "on", code: codeFile.text]

            response.success = parseCodeResults 
            response.'302' = defaultFailure 
            response.failure = defaultFailure
        }
    }

    Closure parseCodeResults = { resp, json ->
        if (!json) {
            println "No JSON returned with output!"
            return
        }
        def htmlParser = new XmlSlurper(new org.cyberneko.html.parsers.SAXParser())
        def unparsedText = json?.exception ? json?.output + "\n" + json?.exception : json?.output
        def newlinedText = unparsedText.replaceAll(/<br\/>/, "\n")  
        def decodedHtml = htmlParser.parseText(newlinedText).text()
        def result = decodedHtml?.replaceAll(/(?m)^groovy> .+\n/, "")
        println result
    }

    Closure defaultFailure = { resp, reader ->
        println "Error!  Unexpected HTTP Status Code returned"
        println "Error status: ${resp.statusLine.statusCode}" 
        println "Got response: ${resp.statusLine}"
        println "Headers: " 
        resp.headers.each { header ->
            println "${header.name} = ${header.value}"
        }
        println reader?.text
    }

    def authenticate() {
        boolean successfulLogin = false

        http.request( POST ) {
            uri.path = authenticationPath
            requestContentType = URLENC
            body = [j_username: username, j_password: password]
            
            response.'302' = { resp, reader ->
                def redirectTo = resp.headers['Location']?.value
                if (!redirectTo.contains(failedAuthenticationPath)) {
                    successfulLogin = true
                } else {
                    defaultFailure(resp, reader)
                }
            }

            response.failure = defaultFailure
        }

        return successfulLogin
    }

    def parseArgs(args) {
        def cli = new CliBuilder(usage: "postCode.groovy [-u username] [-p password] -b baseUrl file")

        cli.with {
            u longOpt: 'user', args: 1, argName: 'user', 'The username to authenticate with, null username/password skips authentication'
            p longOpt: 'password', args: 1, argName: 'password', 'The password to authenticate with'
            b longOpt: 'baseurl', args: 1, argName: 'baseUrl', 'The required base url to auth/post to, ex: http://localhost:8080/myapp/'
        }

        def options = cli.parse(args)

        if (options.'user') username = options.'user' 
        if (options.'password') password = options.'password' 
        if (options.'baseurl') baseUrl = options.'baseurl'

        if (!options.arguments() || !baseUrl) {
            cli.usage()
            System.exit(-1)
        } else {
            codeFilePath =  options.arguments()[0]
        }

    }
}

new GrailsConsoleCodePoster(args).postCodeAfterOptionalAuth()
