#!groovy

load 'common/Constants.groovy'


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
                        echo 'Deploying the app!'
                    }
                }


            echo "INTERMEDIARY RESULT: ${currentBuild.result}"
        }
        post {
            always {
                step([$class: 'CoberturaPublisher', coberturaReportFile: '**/target/scala-2.11/coverage-report/*.xml'])
                step([$class: 'CheckStylePublisher', pattern: 'target/scalastyle-result.xml, target/scala-2.11/scapegoat-report/scapegoat-scalastyle.xml'])
            }
            failure {
                echo 'Something went wrong. The post build actions failed!'

                currentBuild.result = 'FAILURE'

                if (currentBuild.result == 'SUCCESS') {
                    mail body: "SBR API project build finished with status ${currentBuild.result} at the post stage. Found exception: $e" ,
                    from: '${Constants.SENDER_ADDRESS}',
                    replyTo: '${Constants.REPLY_ADDRESS}',
                    subject: 'SBR API: project build failed',
                    to: '${Constants.RECIPIENT_ADDRESS}'
                }
            }
            currentBuild.result = 'SUCCESS'
        }
        echo "RESULT: ${currentBuild.result}"
    }



