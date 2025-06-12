pipeline {
    agent any 
    environment {
        registryCredential = 'aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 933199133172.dkr.ecr.us-east-1.amazonaws.com'
        EXTERNAL_IMAGE = '933199133172.dkr.ecr.us-east-1.amazonaws.com/external:latest'
        INTERNAL_IMAGE = '933199133172.dkr.ecr.us-east-1.amazonaws.com/internal:latest'
        dockerImage = ''
        AWS_REGION = 'us-east-1'
        EKS_CLUSTER_NAME = 'devops'
    }
    stages {
        stage('Building Internal Docker image') {
            steps {
                dir('internal') {
                    script {
                        sh "docker build -t $INTERNAL_IMAGE ."
                    }
                }
            }
        }
        stage('Building External Docker image') {
            steps {
                dir('external') {
                    script {
                        sh "docker build -t $EXTERNAL_IMAGE ."
                    }
                }
            }
        }
        stage('Push Docker Images to ECR Registry') {
            steps {
                script {
                    sh "docker push $INTERNAL_IMAGE"
                    sh "docker push $EXTERNAL_IMAGE"
                }
            }
        }
        stage('deploy to k8s') {
            agent {
                docker { 
                    image 'amazon/aws-cli:2.15.41'
                    args '-e HOME=/tmp'
                    reuseNode true
                }
            }
            steps {
                echo 'Deploying to AWS EKS cluster'
                sh '''
                    # Configure AWS CLI
                    aws eks update-kubeconfig --region $AWS_REGION --name $EKS_CLUSTER_NAME

                    # Deploy to EKS using kubectl
                    kubectl apply -f internal-deployment.yaml
                    kubectl apply -f external-deployment.yaml
                    kubectl apply -f internal-service.yaml
                    kubectl apply -f external-service.yaml
                '''
            }
        }
        stage('Remove local docker image') {
            steps {
                sh "sudo docker rmi $EXTERNAL_IMAGE:latest || true"
                sh "sudo docker rmi $INTERNAL_IMAGE:latest || true"
            }
        }
    }
}