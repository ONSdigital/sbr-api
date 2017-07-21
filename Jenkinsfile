#!groovy
//@Library('jenkins-pipeline-shared@email-sbr') _
@Library('jenkins-pipeline-shared@temporary') _
//@Library('jenkins-pipeline-shared@feature/cloud-foundry-deploy') _

pipeline {
    agent any
    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timeout(time: 30, unit: 'MINUTES')
    }
    stages {
        stage('Checkout'){
            agent any
            steps{
                deleteDir()
                checkout scm
                stash name: 'app'
                sh "$SBT version"
                script {
                    version = '1.0.' + env.BUILD_NUMBER
                    currentBuild.displayName = version
                    // currentBuild.result = "SUCCESS"
                    env.NODE_STAGE = "Checkout"
                }
            }
        }

        stage('Build'){
            agent any
            steps {
                colourText("info", "Building ${env.BUILD_ID} on ${env.JENKINS_URL} from branch ${env.BRANCH_NAME}")
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

        // stage('Patch Release') {
        //   agent { label 'adrianharristesting' }
        //   when {
        //     expression  {
        //         env.BRANCH_NAME =~ patch//.*|fix//.*
        //     }
        //     // branch (patch\/.*|fix\/.*)
        //   }
        //   steps {
        //         script{
        //             env.NODE_STAGE = "Reports"
        //         }
        //         colourText("info", "release version would be increased!")
        //   }
        // }


        stage('Static Analysis') {
          agent any
          steps {
            parallel (
              "Unit" :  {
                colourText("info","Running unit tests")
                sh "$SBT test"
              },
              "Style" : {
                colourText("info","Running style tests")
                sh '''
                $SBT scalastyleGenerateConfig
                $SBT scalastyle
                '''
              },
              "Additional" : {
                colourText("info","Running additional tests")
                sh "$SBT scapegoat"
              }
            )
          }
          post {
            always {
              script {
                  env.NODE_STAGE = "Static Analysis"
              }
            }
            success {
              colourText("info","Generating reports for tests")
            //   junit '**/target/test-reports/*.xml'
            
              step([$class: 'CoberturaPublisher', coberturaReportFile: '**/target/scala-2.11/coverage-report/*.xml'])
              step([$class: 'CheckStylePublisher', pattern: 'target/scalastyle-result.xml, target/scala-2.11/scapegoat-report/scapegoat-scalastyle.xml'])
            }
            failure {
              colourText("warn","Failed to retrieve reports.")
            }
          }
        }


        // bundle all libs and dependencies
        stage ('Bundle') {
            agent any
            // when {
            //     branch "develop"
            // }
            steps {
                script {
                  env.NODE_STAGE = "Bundle"
                }
                colourText("info", "Bundling....")
                dir('conf') {
                    git(url: "$GITLAB_URL/jenkins_public/jenkins-pipeline-shared.git", credentialsId: 'sbr-gitlab-id', branch: 'temporary')
                    // git(url: "$GITLAB_URL/StatBusReg/sbr-api.git", credentialsId: 'sbr-gitlab-id', branch: 'develop')
                }
                // packageApp('dev')
                // packageApp('test')
            // stash name: "zip"
            }
        }


        stage('Deploy Dev'){
            agent any
            // when {
            //     branch "develop"
            // }
            steps {
                colourText("success", 'Deploy Dev.')
                script {
                  env.NODE_STAGE = "Deploy - Dev"
                }
                milestone(1)
                lock('Deployment Initiated') {
                    colourText("info", 'deployment in progress')
                    // unstash zip
                    deployToCloudFoundry('cloud-foundry-sbr-dev-user', 'sbr', 'dev', 'dev-sbr-api', 'dev-ons-sbr-api.zip', 'conf/dev/manifest.yml')
                }
            }
        }

        stage('Integration Tests - Dev') {
            agent { label 'adrianharristesting' }
            when {
                branch "develop"
            }
            steps {
                colourText("success", 'Integration Tests - Dev.')
            }
        }


        stage('Deploy Test'){
            agent any
            when {
                branch "develop"
            }
            steps {
                colourText("success", 'Deploy Test.')
                milestone(1)
                lock('Deployment Initiated') {
                    colourText("info", 'deployment in progress')
                    // deployToCloudFoundry('cloud-foundry-sbr-test-user', 'sbr', 'test', 'test-sbr-api', 'test-ons-sbr-api.zip', 'conf/test/manifest.yml')
                }
            }

        }

        stage('Integration Tests - Test') {
            agent { label 'adrianharristesting' }
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
            agent any
            when {
                branch "master"
            }
            steps {
                colourText("success", 'Release.')
            }
        }

        stage ('Package') {
            agent any
            when {
                branch "master"
            }
            steps {
                colourText("success", 'Package.')
            }

        }

        stage ('Make Artifact') {
            agent any
            when {
                branch "master"
            }
            steps {
                colourText("success", 'Store.')
            }

        }

        stage ('Deploy Live') {
            agent any
            when {
                branch "master"
            }
            steps {
                colourText("success", 'Deploy Live.')
                milestone(1)
                lock('Deployment Initiated') {
                    colourText("info", 'deployment in progress')
                    // deployToCloudFoundry('cloud-foundry-sbr-beta-user', 'sbr', 'beta', 'beta-sbr-api', 'beta-ons-sbr-api.zip', 'conf/beta/manifest.yml')
                }
            }

        }
    }
    post {
        always {
            script {
                colourText("info", 'Post steps initiated')
                deleteDir()
            }

        }
        success {
            colourText("success", "All stages complete. Build was successful.")
            sendNotifications currentBuild.result, "\$SBR_EMAIL_LIST"
        }
        unstable {
            colourText("warn", "Something went wrong, build finished with result ${currentResult}. This may be caused by failed tests, code violation or in some cases unexpected interrupt.")
            sendNotifications currentResult, "\$SBR_EMAIL_LIST", "${env.NODE_STAGE}"
        }
        failure {
            colourText("warn","Process failed at: ${env.NODE_STAGE}")
            sendNotifications currentResult, "\$SBR_EMAIL_LIST", "${env.NODE_STAGE}"
        }
    }
}



// def packageApp(String env) {
//   withEnv(["ENV=${env}"]) {
//     sh '''
//       zip -g $ENV-ons-bi-api.zip conf/$ENV/krb5.conf
//       zip -g $ENV-ons-bi-api.zip conf/$ENV/bi-$ENV-ci.keytab
//     '''
//   }
// }
