#!groovy

pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Building in processing'
                sh '''
                    $SBT clean compile "project api" universal:packageBin coverage test coverageReport
                    cp target/universal/ons-sbr-api-*.zip dev-ons-sbr-api.zip
                    cp target/universal/ons-sbr-api-*.zip test-ons-sbr-api.zip
                '''
            }
        }
        stage('Code Quality') {
            steps {
                sh '''
                    $SBT scapegoat
                    $SBT scalastyle
                '''
            }
        }
        stage('Test') {
            steps {
                echo 'Conducting Tests'
            }
        }

        stage('Deploy') {
            steps {
                echo 'Deploy the app!!'
            }
        }
    }
    post {
        always {
            step([$class: 'CheckStylePublisher', pattern: 'target/scalastyle-result.xml, target/scala-2.11/scapegoat-report/scapegoat-scalastyle.xml'])
        }
        failure {
            echo 'Something went wrong. The post build actions failed!'
        }
    }
}