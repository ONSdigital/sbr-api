#!groovy
@Library('jenkins-pipeline-shared@email-sbr') _
//@Library('jenkins-pipeline-shared@develop') _
//@Library('jenkins-pipeline-shared@feature/cloud-foundry-deploy') _

pipeline {
    agent none

    options {
        skipDefaultCheckout()
    }
    triggers {
        pollSCM('*/5 * * * *') // pollSCM every 5 minutes
    }

    stages {
        stage('Checkout'){
            steps{
                deleteDir()
                checkout scm
                stash name: 'app'
                script {
                    version = '1.0.' + env.BUILD_NUMBER
                    currentBuild.displayName = version
                    currentBuild.result = "SUCCESS"
                }
            }
        }

        stage('Build'){
            agent { label 'adrianharristesting' }
            steps {
                colourText("info", "Building ${env.BUILD_ID} on ${env.JENKINS_URL}")
                script {
                    env.NODE_STAGE = "Build"
                }
                sh '''
                $SBT clean compile "project api" universal:packageBin coverage test coverageReport
                cp target/universal/ons-sbr-api-*.zip dev-ons-sbr-api.zip
                cp target/universal/ons-sbr-api-*.zip test-ons-sbr-api.zip
            '''
            }
        }

        stage('Code Quality'){
            agent { label 'adrianharristesting' }
            steps {
                script {
                    env.NODE_STAGE = "Code Quality"
                }
                sh '''
                $SBT scapegoat
                $SBT scalastyle
                '''
            }

        }

        stage('Test - Functional'){
            agent { label 'adrianharristesting' }
            steps{
                script{
                    env.NODE_STAGE = "Test - Functional"
                }
            }
        }

        stage('Integration Test'){
            agent { label 'adrianharristesting' }
            steps {
                script{
                    env.NODE_STAGE = "Integration Test"
                }
            }
        }

        stage('Reports') {
            agent { label 'adrianharristesting' }
            steps {
                script{
                    env.NODE_STAGE = "Reports"
                }
                step([$class: 'CoberturaPublisher', coberturaReportFile: '**/target/scala-2.11/coverage-report/*.xml'])
                step([$class: 'CheckStylePublisher', pattern: 'target/scalastyle-result.xml, target/scala-2.11/scapegoat-report/scapegoat-scalastyle.xml'])
            }
        }

        stage ('Approve') {
            agent any
            steps {
                script {
                    env.NODE_STAGE = "Approve"
                }
                timeout(time: 2, unit: 'MINUTES') {
                    input message: 'Do you wish to deploy the build?'
                }
            }
        }


        stage('Deploy'){
            agent any
            steps {
                script {
                    env.NODE_STAGE = "Deploy"
                }
                milestone(0)
                lock('Deployment Initiated') {
                    colourText("info", 'deployment in progress')
                }
                colourText("success", 'Deployment Complete.')
            }
        }

        stage('Versioning'){
            steps {
                script {
                    env.NODE_STAGE = "Versioning"
                }
//            sh '''
//            git checkout devops/temp
//            echo version : \\\"0.${env.BUILD_ID}\\\" >> build.sbt
//            git commit -am "Updated version number"
//            '''
            }
        }

        stage('Confirmation'){
            steps {
                script {
                    env.NODE_STAGE = "Confirmation Notification"
                }
                colourText("info", 'All stages complete. Build Successful so far.')
                script {
                    if (getEmailStatus() == true ) {
                        sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST"
                    }
                    else {
                        colourText("info", 'NO email will be sent - email service has been manually turned off!')
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                env.NODE_STAGE = "Post"
            }
        }
        failure {
            script {
                env.NODE_STAGE = "Approve"
            }
//            currentBuild.result = "FAILURE"
            colourText("warn","Process failed at: ${env.NODE_STAGE}")
            script {
                if (getEmailStatus() == true ) {
                    sendNotifications currentResult, ${env.NODE_STAGE}
                }
            }
            colourText("warn", "Build has failed! Stopped on stage: ${env.NODE_STAGE} - NO email will be sent")
        }
    }
}




/*
* @method colourText(level,text)
*
* @description This method will wrap any input text inside
* ANSI colour codes.
*
* @param {String} level - The logging level (warn/info)
* @param {String} text - The text to wrap inside the colour
*
*/
def colourText(level,text){
    wrap([$class: 'AnsiColorBuildWrapper']) {
        // This method wraps input text in ANSI colour
        // Pass in a level, e.g. info or warning
        def code = getLevelCode(level)
        echo "${code[0]}${text}${code[1]}"
    }
}

/*
* @method getLevelCode(level)
*
* @description This method is called with a log level and
* will return a list with the start and end ANSI codes for
* the log level colour.
*
* @param {String} level - The logging level (warn/info)
*
* @return {List} colourCode - [start ANSI code, end ANSI code]
*
*/
def getLevelCode(level) {
    def colourCode
    switch (level) {
        case "info":
            // Blue
            colourCode = ['\u001B[34m','\u001B[0m']
            break
        case "error":
            // Red
            colourCode = ['\u001B[31m','\u001B[0m']
            break
        default:
            colourCode = ['\u001B[31m','\u001B[0m']
            break
    }
    colourCode
}