pipeline {
    agent any

    tools {
        jdk("JAVA 17")
    }

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
        SPRING_JWT_REGISTER_SECRET_KEY = credentials('spring-jwt-base64-register-secret')

        SPRING_REDIS_HOST = credentials('spring-redis-host')
        SPRING_REDIS_PORT = credentials('spring-redis-port')

        SPRING_MAIL_HOST = credentials('spring-mail-host')
        SPRING_MAIL_USERNAME = credentials('spring-mail-username')
        SPRING_MAIL_PASSWORD = credentials('spring-mail-password')
        SPRING_MAIL_PORT = credentials('spring-mail-port')

        SPRING_OAUTH2_GOOGLE_CLIENT_ID = credentials('spring-oauth2-google-client-id')
        SPRING_OAUTH2_GOOGLE_SECRET = credentials('spring-oauth2-google-secret')
        SPRING_OAUTH2_GITHUB_CLIENT_ID = credentials('spring-oauth2-github-client-id')
        SPRING_OAUTH2_GITHUB_SECRET = credentials('spring-oauth2-github-secret')
        SPRING_OAUTH2_FACEBOOK_CLIENT_ID = credentials('spring-oauth2-facebook-client-id')
        SPRING_OAUTH2_FACEBOOK_SECRET = credentials('spring-oauth2-facebook-secret')
        SPRING_OAUTH2_NAVER_CLIENT_ID = credentials('spring-oauth2-naver-client-id')
        SPRING_OAUTH2_NAVER_SECRET = credentials('spring-oauth2-naver-secret')
        SPRING_OAUTH2_KAKAO_CLIENT_ID = credentials('spring-oauth2-kakao-client-id')
        SPRING_OAUTH2_KAKAO_SECRET = credentials('spring-oauth2-kakao-secret')
        SPRING_OAUTH2_DISCORD_CLIENT_ID = credentials('spring-oauth2-discord-client-id')
        SPRING_OAUTH2_DISCORD_SECRET = credentials('spring-oauth2-discord-secret')
        SPRING_OAUTH2_MICROSOFT_CLIENT_ID = credentials('spring-oauth2-microsoft-client-id')
        SPRING_OAUTH2_MICROSOFT_SECRET = credentials('spring-oauth2-microsoft-secret')

        GITHUB_CREDENTIALS_ID = 'git-hub'
        DOCKER_CREDENTIALS_ID = 'docker-hub'
    }

    stages {
        stage('Clone') {
            steps {
                echo 'Clonning Repository'
                git url: 'git@github.com:akon47/hwans-api-server.git', branch: 'master', credentialsId: GITHUB_CREDENTIALS_ID, changelog: false
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
                    prodProperties = prodProperties.replaceAll(/\{jwt-base64-register-secret\}/, SPRING_JWT_REGISTER_SECRET_KEY)
                    prodProperties = prodProperties.replaceAll(/\{redis-host\}/, SPRING_REDIS_HOST)
                    prodProperties = prodProperties.replaceAll(/\{redis-port\}/, SPRING_REDIS_PORT)
                    prodProperties = prodProperties.replaceAll(/\{mail-host\}/, SPRING_MAIL_HOST)
                    prodProperties = prodProperties.replaceAll(/\{mail-username\}/, SPRING_MAIL_USERNAME)
                    prodProperties = prodProperties.replaceAll(/\{mail-password\}/, SPRING_MAIL_PASSWORD)
                    prodProperties = prodProperties.replaceAll(/\{mail-port\}/, SPRING_MAIL_PORT)

                    prodProperties = prodProperties.replaceAll(/\{oauth2-google-client-id\}/, SPRING_OAUTH2_GOOGLE_CLIENT_ID)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-google-secret\}/, SPRING_OAUTH2_GOOGLE_SECRET)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-github-client-id\}/, SPRING_OAUTH2_GITHUB_CLIENT_ID)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-github-secret\}/, SPRING_OAUTH2_GITHUB_SECRET)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-facebook-client-id\}/, SPRING_OAUTH2_FACEBOOK_CLIENT_ID)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-facebook-secret\}/, SPRING_OAUTH2_FACEBOOK_SECRET)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-naver-client-id\}/, SPRING_OAUTH2_NAVER_CLIENT_ID)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-naver-secret\}/, SPRING_OAUTH2_NAVER_SECRET)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-kakao-client-id\}/, SPRING_OAUTH2_KAKAO_CLIENT_ID)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-kakao-secret\}/, SPRING_OAUTH2_KAKAO_SECRET)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-discord-client-id\}/, SPRING_OAUTH2_DISCORD_CLIENT_ID)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-discord-secret\}/, SPRING_OAUTH2_DISCORD_SECRET)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-microsoft-client-id\}/, SPRING_OAUTH2_MICROSOFT_CLIENT_ID)
                    prodProperties = prodProperties.replaceAll(/\{oauth2-microsoft-secret\}/, SPRING_OAUTH2_MICROSOFT_SECRET)

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
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@10.10.10.100 'docker pull ${IMAGE_NAME}'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@10.10.10.100 'docker ps -aq --filter name=${APP_NAME} | grep -q . && docker rm -f \$(docker ps -aq --filter name=${APP_NAME}) || true'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@10.10.10.100 'docker run -d --restart always --name ${APP_NAME} -v /home/kimhwan/blog-datas:/var/attachments -v /secret:/home/kimhwan/blog-secret -v /etc/localtime:/etc/localtime:ro -v /usr/share/zoneinfo/Asia/Seoul:/etc/timezone:ro --net=host ${IMAGE_NAME}'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@10.10.10.100 'docker images -qf dangling=true | xargs -I{} docker rmi {} || true'"
                    sh "ssh -o StrictHostKeyChecking=no kimhwan@10.10.10.100 'docker rmi ${IMAGE_NAME}:${IMAGE_TAG} || true'"
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
