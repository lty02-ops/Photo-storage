def canDeploy() {
    return params.DEPLOY_TARGET in ['aws', 'gcp', 'both'] && !env.CHANGE_ID &&
        (env.BRANCH_NAME == 'main' || env.SOURCE_BRANCH in ['origin/main', 'main'])
}

def runDeployment(action) {
    def targets = params.DEPLOY_TARGET == 'both' ? ['aws', 'gcp'] : [params.DEPLOY_TARGET]

    for (cloud in targets) {
        def credentialVariable = cloud == 'aws' ? 'AWS_SHARED_CREDENTIALS_FILE' : 'GCP_CREDENTIALS_FILE'
        withCredentials([file(credentialsId: "photo-storage-${cloud}-credentials", variable: credentialVariable)]) {
            sh label: "${cloud}: ${action}",
                script: "set +x\nbash infra/jenkins/deploy-app.sh ${cloud} ${action}"
        }
    }
}

pipeline {
    agent { label 'photo-storage-ci' }

    triggers {
        pollSCM('H/5 * * * *')
    }

    parameters {
        choice(name: 'DEPLOY_TARGET', choices: ['none', 'gcp', 'aws', 'both'],
            description: 'Deployment target (none: CI only)')
    }

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
        timeout(time: 60, unit: 'MINUTES')
    }

    environment {
        CI = 'true'
        TF_IN_AUTOMATION = 'true'
        TF_INPUT = 'false'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    def source = checkout scm
                    env.SOURCE_BRANCH = source.GIT_BRANCH ?: ''
                    env.IMAGE_TAG = "${source.GIT_COMMIT.take(12)}-${env.BUILD_NUMBER}"
                    if (canDeploy()) {
                        stash name: 'deploy-source',
                            includes: 'frontend/**,backend/**,infra/helm/photo-storage/**,infra/jenkins/deploy-app.sh',
                            excludes: '**/node_modules/**,**/target/**,**/dist/**,**/.env,**/.env.*,**/secret-values.yaml'
                    }
                }
            }
        }

        stage('Backend Test') {
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

        stage('Frontend Build') {
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

        stage('Validate') {
            steps {
                sh '''
                    set -eu
                    for cloud in aws gcp local; do
                        helm lint infra/helm/photo-storage \
                          -f "infra/helm/photo-storage/values-${cloud}.yaml"
                    done
                    for cloud in aws gcp multicloud-network; do
                        terraform -chdir="infra/terraform/${cloud}" fmt -check
                        terraform -chdir="infra/terraform/${cloud}" init -backend=false -input=false -lockfile=readonly
                        terraform -chdir="infra/terraform/${cloud}" validate -no-color
                    done
                '''
            }
        }

        stage('Deploy') {
            when {
                beforeAgent true
                expression { canDeploy() }
            }
            agent { label 'photo-storage-deploy' }
            environment {
                AWS_ACCOUNT_ID = '022868553365'
                AWS_REGION = 'ap-northeast-2'
                GCP_PROJECT_ID = 'photo-storage-503901'
                GCP_REGION = 'asia-northeast3'
            }
            stages {
                stage('Docker Build') {
                    steps {
                        deleteDir()
                        unstash 'deploy-source'
                        sh '''
                            set -eu
                            docker build -t "photo-storage-frontend:$IMAGE_TAG" ./frontend
                            docker build -t "photo-storage-backend:$IMAGE_TAG" ./backend
                        '''
                    }
                }
                stage('Image Push') {
                    steps {
                        script { runDeployment('publish') }
                    }
                }
                stage('Helm Deploy') {
                    steps {
                        script { runDeployment('deploy') }
                    }
                }
            }
            post {
                always {
                    sh '''
                        docker image rm "photo-storage-frontend:$IMAGE_TAG" "photo-storage-backend:$IMAGE_TAG" >/dev/null 2>&1 || true
                    '''
                    deleteDir()
                }
            }
        }
    }
}
