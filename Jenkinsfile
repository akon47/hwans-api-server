pipeline {
    agent any

    environment {
        dockerImage = ''
        APP_NAME = 'hwans-api-server'
        IMAGE_NAME = 'akon47/hwans-api-server'
        IMAGE_TAG = "${env.APP_VERSION}.${env.BUILD_NUMBER}"
        ACTIVE_PROFILE = 'prod'
        SPRING_PROD_PROPERTIES_PATH = "src/main/resources/application-${ACTIVE_PROFILE}.yml"
        SPRING_DATASOURCE_URL = credentials('spring-datasource-url')
        SPRING_DATASOURCE_USERNAME = credentials('spring-datasource-username')
        SPRING_DATASOURCE_PASSWORD = credentials('spring-datasource-password')
        SPRING_JWT_ACCESS_SECRET_KEY = credentials('spring-jwt-base64-access-secret')
        SPRING_JWT_REFRESH_SECRET_KEY = credentials('spring-jwt-base64-refresh-secret')
        SPRING_REDIS_HOST = credentials('spring-redis-host')
        SPRING_REDIS_PORT = credentials('spring-redis-port')
        SPRING_MAIL_HOST = 'host'
        SPRING_MAIL_USERNAME = 'username'
        SPRING_MAIL_PASSWORD = 'password'
        SPRING_MAIL_PORT = '25'
        GITHUB_CREDENTIALS_ID = 'git-hub'
        DOCKER_CREDENTIALS_ID = 'docker-hub'
    }

    stages {
        stage('Clone') {
            steps {
                echo 'Clonning Repository'
                git url: 'git@github.com:akon47/hwans-api-server.git', branch: 'master', credentialsId: GITHUB_CREDENTIALS_ID
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
        
        stage('Prepare') {
            steps {
                echo "Pre-Processing for ${ACTIVE_PROFILE} profile"
                script {
                    prodProperties = readFile file: SPRING_PROD_PROPERTIES_PATH
                    prodProperties = prodProperties.replaceAll(/\{datasource-url\}/, SPRING_DATASOURCE_URL)
                    prodProperties = prodProperties.replaceAll(/\{datasource-username\}/, SPRING_DATASOURCE_USERNAME)
                    prodProperties = prodProperties.replaceAll(/\{datasource-password\}/, SPRING_DATASOURCE_PASSWORD)
                    prodProperties = prodProperties.replaceAll(/\{jwt-base64-access-secret\}/, SPRING_JWT_ACCESS_SECRET_KEY)
                    prodProperties = prodProperties.replaceAll(/\{jwt-base64-refresh-secret\}/, SPRING_JWT_REFRESH_SECRET_KEY)
                    prodProperties = prodProperties.replaceAll(/\{redis-host\}/, SPRING_REDIS_HOST)
                    prodProperties = prodProperties.replaceAll(/\{redis-port\}/, SPRING_REDIS_PORT)
                    prodProperties = prodProperties.replaceAll(/\{mail-host\}/, SPRING_MAIL_HOST)
                    prodProperties = prodProperties.replaceAll(/\{mail-username\}/, SPRING_MAIL_USERNAME)
                    prodProperties = prodProperties.replaceAll(/\{mail-password\}/, SPRING_MAIL_PASSWORD)
                    prodProperties = prodProperties.replaceAll(/\{mail-port\}/, SPRING_MAIL_PORT)

                    writeFile file: SPRING_PROD_PROPERTIES_PATH, text: prodProperties
                }
            }
            post {
                success {
                    echo 'Successfully Pre-Processing'
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
                    sh 'chmod +x gradlew'
                    sh "SPRING_PROFILES_ACTIVE=${ACTIVE_PROFILE} ./gradlew clean build -x test --info"
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
                    docker.withRegistry('', DOCKER_CREDENTIALS_ID) {
                        dockerImage.push("${IMAGE_TAG}")
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
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker ps -q -a --filter name=${APP_NAME} | grep -q . && docker rm -f \$(docker ps -aq --filter name=${APP_NAME}) || true'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker run -d --name ${APP_NAME} -p 1200:8080 --network hwans-api-server-net ${IMAGE_NAME}'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker images -qf dangling=true | xargs -I{} docker rmi {} || true'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@kimhwan.kr 'docker rmi ${IMAGE_NAME}:${IMAGE_TAG} || true'"
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
