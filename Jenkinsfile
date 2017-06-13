pipeline{
    agent any
    stages{
        stage ('Build') {
            steps {
                echo 'Building in processing'
                sh '''
                    $SBT clean compile "project api" universal:packageBin coverage test coverageReport
                    cp api/target/universal/ons-bi-api-*.zip api/target/universal/ons-bi-api.zip
                    rm api/target/universal/ons-bi-api-*.zip
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