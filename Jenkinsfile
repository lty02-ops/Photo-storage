pipeline {
    agent { label 'photo-storage-ci' }

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        CI = 'true'
        TF_IN_AUTOMATION = 'true'
        TF_INPUT = 'false'
    }

    stages {
        stage('소스 가져오기') {
            steps {
                checkout scm
            }
        }

        stage('실행 도구 확인') {
            steps {
                sh '''
                    set -eu
                    java -version
                    mvn --version
                    node --version
                    npm --version
                    helm version --short
                    terraform version
                '''
            }
        }

        stage('백엔드 테스트') {
            steps {
                dir('backend') {
                    sh 'mvn --batch-mode clean test'
                }
            }
            post {
                always {
                    junit testResults: 'backend/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('프론트엔드 빌드') {
            steps {
                dir('frontend') {
                    sh 'npm ci --no-audit --no-fund'
                    sh 'npm run build'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'frontend/dist/**', fingerprint: true
                }
            }
        }

        stage('Helm 검사') {
            steps {
                sh '''
                    set -eu
                    for cloud in aws gcp local; do
                        helm lint infra/helm/photo-storage \
                          -f "infra/helm/photo-storage/values-${cloud}.yaml"
                    done
                '''
            }
        }

        stage('Terraform 검증') {
            steps {
                sh '''
                    set -eu
                    for cloud in aws gcp multicloud-network; do
                        terraform -chdir="infra/terraform/${cloud}" fmt -check
                        terraform -chdir="infra/terraform/${cloud}" init -backend=false -input=false -lockfile=readonly
                        terraform -chdir="infra/terraform/${cloud}" validate -no-color
                    done
                '''
            }
        }
    }
}
