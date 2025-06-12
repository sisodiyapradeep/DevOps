pipeline {
    agent any 
    environment {
        registryCredential = 'aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 933199133172.dkr.ecr.us-east-1.amazonaws.com'
        EXTERNAL_IMAGE = '933199133172.dkr.ecr.us-east-1.amazonaws.com/external'
        IMAGE_TAG = 'latest'
        INTERNAL_IMAGE = '933199133172.dkr.ecr.us-east-1.amazonaws.com/internal'
        dockerImage = ''
        AWS_REGION = 'us-east-1'
        CLUSTER_NAME = 'devops'
        NAMESPACE = 'devops'
    }
    stages {
        stage('Building Internal Docker image') {
            steps {
                dir('internal') {
                    script {
                        sh "docker build -t ${INTERNAL_IMAGE}:${IMAGE_TAG} ."
                    }
                }
            }
        }
        stage('Building External Docker image') {
            steps {
                dir('external') {
                    script {
                        sh "docker build -t ${EXTERNAL_IMAGE}:${IMAGE_TAG} ."
                    }
                }
            }
        }
        stage('Push Docker Images to ECR Registry') {
            steps {
                withAWS(credentials: 'aws-creds', region: "${AWS_REGION}") {
                    sh '''
                        aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${INTERNAL_IMAGE}
                        docker push ${INTERNAL_IMAGE}:${IMAGE_TAG}
                        aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${EXTERNAL_IMAGE}
                        docker push ${EXTERNAL_IMAGE}:${IMAGE_TAG}
                    '''
                }
            }
        }
        stage('deploy to k8s') {
            steps {
                withAWS(credentials: 'aws-creds', region: "${AWS_REGION}") {
                    sh '''
                        aws sts get-caller-identity
                        aws eks update-kubeconfig --region $AWS_REGION --name $CLUSTER_NAME
                        kubectl get nodes
                        kubectl get namespace $NAMESPACE || kubectl create namespace $NAMESPACE
                        kubectl config set-context --current --namespace=$NAMESPACE
                        kubectl apply -f internal-deployment.yaml
                        kubectl apply -f external-deployment.yaml
                        kubectl apply -f internal-service.yaml
                        kubectl apply -f external-service.yaml
                    '''
                }
            }
        }
        stage('Test Deployment') {
            steps {
                script {
                    sh '''
                        kubectl rollout status deployment/internal-deployment
                        kubectl rollout status deployment/external-deployment
                        kubectl get pods
                    '''
                }
            }
        }
    }
}