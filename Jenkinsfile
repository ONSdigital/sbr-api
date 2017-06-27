#!groovy
//@Library('jenkins-pipeline-shared@email-sbr') _
@Library('jenkins-pipeline-shared@develop') _
//@Library('jenkins-pipeline-shared@feature/cloud-foundry-deploy') _

pipeline {
    agent any
    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
    }
    stages {
        stage('Checkout'){

            steps{
                checkout scm
                stash name: 'app'
                script {
                    version = '1.0.' + env.BUILD_NUMBER
                    currentBuild.displayName = version
                    currentBuild.result = "SUCCESS"
                    env.NODE_STAGE = "Checkout"
                }
            }
        }

        stage('Build'){

            steps {
                colourText("info", "Building ${env.BUILD_ID} on ${env.JENKINS_URL} from branch ${env.BRANCH_NAME}")
                script {
                    env.NODE_STAGE = "Build"
                }
//                sh '''
//                $SBT clean compile "project api" universal:packageBin coverage test coverageReport
//                cp target/universal/ons-sbr-api-*.zip dev-ons-sbr-api.zip
//                cp target/universal/ons-sbr-api-*.zip test-ons-sbr-api.zip
//                '''
            }
        }

        stage('Static Analysis'){
            steps {
                script {
                    env.NODE_STAGE = "Static Analysis"
                }
//                sh '''
//                $SBT scapegoat
//                $SBT scalastyle
//                '''
            }
        }

        stage('Reports') {

            steps {
                script{
                    env.NODE_STAGE = "Reports"
                }
//                step([$class: 'CoberturaPublisher', coberturaReportFile: '**/target/scala-2.11/coverage-report/*.xml'])
//                step([$class: 'CheckStylePublisher', pattern: 'target/scalastyle-result.xml, target/scala-2.11/scapegoat-report/scapegoat-scalastyle.xml'])
            }
        }


        // bundle all libs and dependencies
        stage ('Bundle') {
            when {
                branch "develop"
            }
            steps {
                colourText("info", "Bundling....")
//                dir('conf') {
//                    git(url: "$GITLAB_URL/StatBusReg/sbr-api.git", credentialsId: 'sbr-gitlab-id', branch: 'develop')
//                }
//
//                packageApp('dev')
//                packageApp('test')
            }
        }


        stage('Deploy Dev'){
            when {
                branch "develop"
            }
            steps {
                colourText("success", 'Deploy Dev.')
                milestone(1)
                lock('Deployment Initiated') {
                    colourText("info", 'deployment in progress')
                }
            }

        }

        stage('Integration Tests - Dev') {
            when {
                branch "develop"
            }
            steps {
                colourText("success", 'Integration Tests - Dev.')
            }
        }


        stage('Deploy Test'){
            when {
                branch "develop"
            }
            steps {
                colourText("success", 'Deploy Test.')
//                milestone(1)
                lock('Deployment Initiated') {
                    colourText("info", 'deployment in progress')
                }
            }

        }

        stage('Integration Tests - Test') {
            when {
                branch "develop"
            }
            steps {
                colourText("success", 'Integration Tests - Test.')
            }
        }


        stage ('Approve') {
            agent { label 'adrianharristesting' }
            when {
                branch "master"
            }
            steps {
                script {
                    env.NODE_STAGE = "Approve"
                }
                timeout(time: 2, unit: 'MINUTES') {
                    input message: 'Do you wish to deploy the build?'
                }
            }
        }


        stage ('Release') {
            when {
                branch "master"
            }
            steps {
                colourText("success", 'Release.')
            }
        }

        stage ('Package') {
            when {
                branch "master"
            }
            steps {
                colourText("success", 'Package.')
            }

        }

        stage ('Make Artifact') {
            when {
                branch "master"
            }
            steps {
                colourText("success", 'Store.')
            }

        }

        stage ('Deploy Live') {
            when {
                branch "master"
            }
            steps {
                colourText("success", 'Deploy Live.')
                milestone(1)
                lock('Deployment Initiated') {
                    colourText("info", 'deployment in progress')
                }
            }

        }
    }
    post {
        always {
            script {
                env.NODE_STAGE = "Post"
            }
            deleteDir()
            colourText("info", 'Post steps initiated')

        }
        success {
            colourText("success", 'All stages complete. Build was successful.')
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST"
        }
        unstable {
            colourText("warn", 'Something went wrong, build finished with result ${currentResult}. This may be caused by failed tests, code violation or in some cases unexpected interrupt.')
            sendNotifications currentResult, "\$SBR_EMAIL_LIST", ${env.NODE_STAGE}
        }
        failure {
//            currentBuild.result = "FAILURE"
            colourText("warn",'Process failed at: ${env.NODE_STAGE}')
            sendNotifications currentResult, "\$SBR_EMAIL_LIST", ${env.NODE_STAGE}
        }
    }
}


