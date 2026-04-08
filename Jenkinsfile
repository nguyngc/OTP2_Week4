pipeline {
    agent any

    tools {
        maven 'Maven 3.9.12'
    }

    stages {
        stage ('Checkout') {
            steps {
                git branch: 'main',
                credentialsId: 'github-pat',
                url: 'https://github.com/nguyngc/OTP2_Week4.git'
            }
        }
        stage ('Build Job') {
            steps {
                sh 'mvn clean install'
            }
        }
        stage ('Run Test') {
            steps {
                sh 'mvn test'
            }
        }
        stage ('Code Coverage') {
            steps {
                sh 'mvn jacoco:report'
            }
        }
        stage ('Publish Test Result') {
            steps {
                junit '**/target/surefire-reports/*.xml'
            }
        }
        stage ('Publish Coverage Report') {
            steps {
                jacoco()
            }
        }
        stage('Build Docker Image') {
            environment {
                PATH = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}"
                DOCKERHUB_REPO = 'nguyngc/otp2_week4'
                DOCKER_IMAGE_TAG = 'latest'
            }
            steps {
                script {
                    docker.build("${DOCKERHUB_REPO}:${DOCKER_IMAGE_TAG}")
                }
            }
        }

        stage('Push Docker Image to Docker Hub') {
            environment {
                PATH = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}"
                DOCKERHUB_CREDENTIALS_ID = 'DockerID'
                DOCKERHUB_REPO = 'nguyngc/otp2_week4'
                DOCKER_IMAGE_TAG = 'latest'
            }
            steps {
                withCredentials([usernamePassword(credentialsId: DOCKERHUB_CREDENTIALS_ID, usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
                    sh '''
                        /usr/local/bin/docker login -u "$DH_USER" -p "$DH_PASS"
                        /usr/local/bin/docker push ${DOCKERHUB_REPO}:${DOCKER_IMAGE_TAG}
                    '''
                }
            }
        }
    }
}