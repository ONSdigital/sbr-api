#!groovy

pipeline{
    agent any
    stages{
        stage ('Build') {
            steps {
                echo 'Building in processing'
                sh '''
                    $SBT clean compile "project api" universal:packageBin coverage test coverageReport
                    cp target/universal/ons-sbr-api-*.zip dev-ons-sbr-api.zip
                    cp target/universal/ons-sbr-api-*.zip test-ons-sbr-api.zip
                '''
            }
        }
        stage ('Test') {
            steps {
                echo 'Conducting Tests'
            }
        }

        stage ('Deploy') {
            steps {
                echo 'Deploy the app!!'
            }
        }
    }
}