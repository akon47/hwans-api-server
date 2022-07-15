pipeline {
    agent any

    environment {
        dockerImage = ''
        TAG = "${env.APP_VERSION}.${env.BUILD_NUMBER}"
        IMAGE_NAME = 'akon47/hwans-api-server'
        SPRING_PROD_PROPERTIES_PATH = 'src/main/resources/application-prod.properties'
        SPRING_DATASOURCE_URL = credentials('spring-datasource-url')
        SPRING_DATASOURCE_USERNAME = credentials('spring-datasource-username')
        SPRING_DATASOURCE_PASSWORD = credentials('spring-datasource-password')
    }

    stages {
        stage('Prepare') {
            steps {
                echo 'Clonning Repository'
                git url: 'git@github.com:akon47/hwans-api-server.git', branch: 'master', credentialsId: 'git-hub'

                echo 'Replace to prod information'
                script {
                    prodProperties = readFile file: "${SPRING_PROD_PROPERTIES_PATH}"
                    prodProperties = prodProperties.replaceAll(/{datasource-url}/, SPRING_DATASOURCE_URL)
                    prodProperties = prodProperties.replaceAll(/{datasource-url}/, SPRING_DATASOURCE_USERNAME)
                    prodProperties = prodProperties.replaceAll(/{datasource-url}/, SPRING_DATASOURCE_PASSWORD)
                    writeFile file: "${SPRING_PROD_PROPERTIES_PATH}", text: prodProperties
                }
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
                    dockerImage = docker.build("${IMAGE_NAME}")
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
                    docker.withRegistry('', 'docker-hub') {
                        dockerImage.push("${TAG}")
                        dockerImage.push("latest")
                    }
                }
            }
            post {
                success {
                    sh 'docker rmi $(docker images -q -f dangling=true) || true'
                }
                failure {
                    error 'This pipeline stops here...'
                }
            }
        }

        stage('Docker Run') {
            steps {
                echo 'Pull Docker Image & Docker Image Run'
                sshagent(credentials: ['ssh']) {
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker pull ${IMAGE_NAME}'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker ps -q --filter name=hwans-api-server | grep -q . && docker rm -f \$(docker ps -aq --filter name=hwans-api-server) || true'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker run -d --name hwans-api-server -p 1200:8080 ${IMAGE_NAME}'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker images -qf dangling=true | xargs -I{} docker rmi {} || true'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker rmi ${IMAGE_NAME}:${TAG} || true'"
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
