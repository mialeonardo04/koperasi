pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '3'))
    }

    triggers {
        // Dipicu webhook GitHub (lewat smee.io -> Jenkins)
        githubPush()
        // Cadangan jika webhook belum jalan: hapus tanda komentar di bawah
        // pollSCM('H/5 * * * *')
    }

    environment {
        REGISTRY   = '192.168.78.20:5000'
        IMAGE_NAME = 'koperasi'
        NAMESPACE  = 'koperasi-dev'
        DEPLOYMENT = 'koperasi'
    }

    stages {
        stage('Prepare') {
            steps {
                script {
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
                    env.IMAGE     = "${env.REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                }
                echo "Image: ${env.IMAGE}"
            }
        }

        stage('Build & Test') {
            steps {
                sh 'chmod +x mvnw'
                // Integration test (Testcontainers) dilewati dulu.
                // Untuk menjalankannya: ganti dengan './mvnw -B verify'
                sh './mvnw -B -DskipITs package'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                sh 'docker build -t "$IMAGE" .'
                sh 'docker push "$IMAGE"'
            }
            post {
                always {
                    // Hapus image lokal supaya disk tidak penuh (image tetap ada di registry)
                    sh 'docker rmi "$IMAGE" || true'
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-koperasi-dev', variable: 'KUBECONFIG')]) {
                    sh '''
                        set -e
                        # Ganti tag image di manifest (hanya di workspace, tidak di-commit)
                        sed -i "s|image: ${REGISTRY}/${IMAGE_NAME}:.*|image: ${IMAGE}|" kube/deployment.yaml
                        grep -n "image:" kube/deployment.yaml

                        kubectl apply -f kube/

                        if ! kubectl -n "${NAMESPACE}" rollout status "deployment/${DEPLOYMENT}" --timeout=300s; then
                            echo "Rollout gagal, rollback ke versi sebelumnya"
                            kubectl -n "${NAMESPACE}" rollout undo "deployment/${DEPLOYMENT}" || true
                            exit 1
                        fi
                    '''
                }
            }
        }
    }

    post {
        success { echo "Deploy sukses: ${env.IMAGE}" }
        failure { echo 'Pipeline gagal, lihat Console Output' }
    }
}
