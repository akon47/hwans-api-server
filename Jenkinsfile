pipeline {
    agent any

    environment {
        dockerImage = ''
        version = "${env.APP_VERSION}.${env.BUILD_NUMBER}"
    }

    stages {
        stage('Prepare') {
            steps {
                echo 'Clonning Repository'
                git url: 'git@github.com:akon47/hwans-api-server.git',
                    branch: 'master',
                    credentialsId: 'git-hub'
            }
            post {
                success {
                    echo 'Successfully Cloned Repository'
                }
                failure {
                    error 'This pipeline stops here...'
                }
            }
        }

        stage('Bulid Gradle') {
            steps {
                echo 'Bulid Gradle'
                dir('.') {
                    sh '''
                        chmod +x gradlew
                        ./gradlew clean build
                    '''
                }
            }
            post {
                failure {
                    error 'This pipeline stops here...'
                }
            }
        }

        stage('Bulid Docker') {
            steps {
                echo 'Bulid Docker'
                script {
                    dockerImage = docker.build('akon47/hwans-api-server')
                }
            }
            post {
                failure {
                    error 'This pipeline stops here...'
                }
            }
        }

        stage('Push Docker') {
            steps {
                echo 'Push Docker'
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-hub') {
                        dockerImage.push(version)
                        dockerImage.push("latest")
                    }
                }
            }
            post {
                failure {
                    error 'This pipeline stops here...'
                }
            }
        }

        stage('Docker Run') {
            steps {
                echo 'Pull Docker Image & Docker Image Run'
                sshagent(credentials: ['ssh']) {
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker pull akon47/hwans-api-server'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker ps -q --filter name=hwans-api-server | grep -q . && docker rm -f \$(docker ps -aq --filter name=hwans-api-server) || true'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker run -d --name hwans-api-server -p 1200:8080 akon47/hwans-api-server:latest'"
                }
            }
            post {
                failure {
                    error 'This pipeline stops here...'
                }
            }
        }
    }
}
