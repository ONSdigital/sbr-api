#!groovy



node() {
    checkout scm
    def constants = load "common/Constants.groovy"

    try {

        stage('Configure'){
            version = '1.0.' + env.BUILD_NUMBER
            currentBuild.displayName = version
            currentBuild.result = "SUCCESS"
        }

        stage('Build'){
            constants.colourText("info", "Building ${env.BUILD_ID} on ${env.JENKINS_URL}")

            env.NODE_STAGE = "Build"

//            sh '''
//                $SBT clean compile "project api" universal:packageBin coverage test coverageReport
//                cp target/universal/ons-sbr-api-*.zip dev-ons-sbr-api.zip
//                cp target/universal/ons-sbr-api-*.zip test-ons-sbr-api.zip
//            '''
        }

        stage('Code Quality'){

            env.NODE_STAGE = "Code Quality"

//            sh '''
//                $SBT scapegoat
//                $SBT scalastyle
//            '''

        }

        stage('Test - Functional'){
            env.NODE_STAGE = "Test - Functional"

        }

        stage('Integration Test'){
            env.NODE_STAGE = "Integration Test"

        }

        stage('Post Actions') {
            env.NODE_STAGE = "Post Actions"
//            step([$class: 'CoberturaPublisher', coberturaReportFile: '**/target/scala-2.11/coverage-report/*.xml'])
//            step([$class: 'CheckStylePublisher', pattern: 'target/scalastyle-result.xml, target/scala-2.11/scapegoat-report/scapegoat-scalastyle.xml'])
        }

        stage ('Approve') {
            env.NODE_STAGE = "Approve"
            timeout(time: 2, unit: 'MINUTES') {
                input message: 'Do you wish to deploy the build?'
            }
        }


        stage('Deploy'){
            env.NODE_STAGE = "Deploy"
            // aborts old pipeline deployment processes
            milestone()
            // only one execution allowed - no parallel (deployments)
            lock('Deployment Initiated') {
                constants.colourText("info", 'deployment in progress')
            }
            constants.colourText("success", 'Deployment Complete.')
        }

        stage('Versioning'){
//            sh '''
//            git checkout devops/temp
//            echo version : \\\"0.${env.BUILD_ID}\\\" >> build.sbt
//            git commit -am "Updated version number"
//            '''
        }

        stage('Confirmation'){

            constants.colourText("info", 'All stages complete. Build Successful so far.')

            if (constants.getEmailStatus() == true ) {
                mail body: 'project build successful',
                        from: constants.getSender(),
                        replyTo: constants.getReplyAddress(),
                        subject: 'project build successful',
                        to: constants.getRecipient()
            }
            else {
                constants.colourText("info", 'NO email will be sent - email service has been manually turned off!')
            }
        }



    }
    catch (err) {
        currentBuild.result = "FAILURE"
        constants.colourText("warn","Process failed at: ${env.NODE_STAGE}")
        if (constants.getEmailStatus() == true ) {
            mail body: "project build error is here: ${env.BUILD_URL}",
                    from: constants.getSender(),
                    replyTo: constants.getReplyAddress(),
                    subject: 'project build failed',
                    to: constants.getRecipient()
        }
        else {
            throw err
        }

        throw err
    }

}