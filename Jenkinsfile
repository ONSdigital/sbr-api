#!groovy

pipeline{
    agent any
    stages{
        stage ('Build') {
            steps {
                echo 'Building in processing'
                sh '''
                    $SBT clean compile "project api" universal:packageBin coverage test coverageReport
                    cp api/target/universal/ons-sbr-api-*.zip dev-ons-bi-api.zip
                    cp api/target/universal/ons-sbr-api-*.zip test-ons-bi-api.zip
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