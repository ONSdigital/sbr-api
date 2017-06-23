#!groovy
@Library('jenkins-pipeline-shared@email-sbr') _
//@Library('jenkins-pipeline-shared@develop') _
//@Library('jenkins-pipeline-shared@feature/cloud-foundry-deploy') _

pipeline {
    agent any
	options {
    		buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
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

        stage('Static Analysis'){
            steps {
                script {
                    env.NODE_STAGE = "Static Analysis"
                }
                sh '''
                $SBT scapegoat
                $SBT scalastyle
                '''
            }
        }


        // bundle all libs and dependencies
        stage ('Bundle') {
            steps {
                dir('conf') {
                    git(url: "$GITLAB_URL/StatBusReg/sbr-api.git", credentialsId: 'sbr-gitlab-id', branch: 'develop')
                }

                packageApp('dev')
                packageApp('test')
            }
        }

        stage('Reports') {

            steps {
                script{
                    env.NODE_STAGE = "Reports"
                }
                step([$class: 'CoberturaPublisher', coberturaReportFile: '**/target/scala-2.11/coverage-report/*.xml'])
                step([$class: 'CheckStylePublisher', pattern: 'target/scalastyle-result.xml, target/scala-2.11/scapegoat-report/scapegoat-scalastyle.xml'])
            }
        }

        stage ('Approve') {

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

            steps {
                script {
                    env.NODE_STAGE = "Deploy"
                }
                milestone(1)
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
            colourText("info", 'Post steps initiated')
        }
        success {
            colourText("success", 'All stages complete. Build was successful.')
            script {
                if (getEmailStatus() == true ) {
                    sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST"
                }
                else {
                    colourText("info", 'NO email will be sent - email service has been manually turned off!')
                }
            }
        }
        unstable {
            colourText("warn", 'Something went wrong, build finished with result ${currentResult}. This may be caused by failed tests, code violation or in some cases unexpected interrupt.')
            script {
                if (getEmailStatus() == true ) {
                    sendNotifications currentResult, "\$SBR_EMAIL_LIST", ${env.NODE_STAGE}
                }
                else {
                    colourText("info", 'NO email will be sent - email service has been manually turned off!')
                }
            }

        }
        failure {
//            currentBuild.result = "FAILURE"
            colourText("warn",'Process failed at: ${env.NODE_STAGE}')
            script {
                if (getEmailStatus() == true ) {
                    sendNotifications currentResult, "\$SBR_EMAIL_LIST", ${env.NODE_STAGE}
                }
            }
            colourText("warn", 'Build has failed! Stopped on stage: ${env.NODE_STAGE} - NO email will be sent')
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


def packageApp(String env) {
    withEnv(["ENV=${env}"]) {
        sh '''
			  zip -g $ENV-ons-bi-api.zip conf/$ENV/krb5.conf
			  zip -g $ENV-ons-bi-api.zip conf/$ENV/bi-$ENV-ci.keytab
		'''
    }
}
