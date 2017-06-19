#!groovy



node() {
    checkout scm
    def constants = load "common/Constants.groovy"

//    constants.test()
//    constants.getSender()


    try {

        stage('Configure'){
            version = '1.0.' + env.BUILD_NUMBER
            currentBuild.displayName = version
            currentBuild.result = "SUCCESS"
        }

        stage('Build'){
            constants.colourText("info", "Building ${env.BUILD_ID} on ${env.JENKINS_URL}")

            env.NODE_STAGE = "Build"

            sh '''
                $SBT clean compile "project api" universal:packageBin coverage test coverageReport
                cp target/universal/ons-sbr-api-*.zip dev-ons-sbr-api.zip
                cp target/universal/ons-sbr-api-*.zip test-ons-sbr-api.zip
            '''
        }

        stage('Code Quality'){

            env.NODE_STAGE = "Code Quality"

            sh '''
                $SBT scapegoat
                $SBT scalastyle
            '''

        }

        stage('Test - Functional'){

            env.NODE_STAGE = "Test - Functional"

        }

        stage('Integration Test'){

            env.NODE_STAGE = "Integration Test"

        }

        stage('Post Actions') {
            echo 'post actions'
            env.NODE_STAGE = "Post Actions"
            step([$class: 'CoberturaPublisher', coberturaReportFile: '**/target/scala-2.11/coverage-report/*.xml'])
            step([$class: 'CheckStylePublisher', pattern: 'target/scalastyle-result.xml, target/scala-2.11/scapegoat-report/scapegoat-scalastyle.xml'])
        }

        stage ('Approve') {
            echo 'approve'
            env.NODE_STAGE = "Approve"
            timeout(time: 3, unit: 'DAYS') {
                input message: 'Do you wish to deploy the build?'
            }
        }


        stage('Deploy'){
            env.NODE_STAGE = "Deploy"
            echo 'reached deploy'
        }

        stage('Confirmation'){

            echo 'All stages complete. Build Successful so far.'

            if (constants.getEmailStatus() == true ) {
                mail body: 'project build successful',
                        from: 'xxxx@yyyyy.com',
                        replyTo: 'xxxx@yyyy.com',
                        subject: 'project build successful',
                        to: 'yyyyy@yyyy.com'
            }
            else {
                echo 'NO email will be sent - email service has been manually turned off!'
            }
        }



    }
    catch (err) {
        currentBuild.result = "FAILURE"
        constants.colourText("warn","Process failed at: ${env.NODE_STAGE}")
        print "Process failed at: ${env.NODE_STAGE}"
        if (constants.getEmailStatus() == true ) {
            mail body: "project build error is here: ${env.BUILD_URL}",
                    from: 'xxxx@yyyy.com',
                    replyTo: 'yyyy@yyyy.com',
                    subject: 'project build failed',
                    to: 'zzzz@yyyyy.com'
        }
        else {
            throw err
        }

        throw err
    }

}