pipeline {
    agent any 
    environment {
        registryCredential = 'aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 933199133172.dkr.ecr.us-east-1.amazonaws.com'
        imageName = '933199133172.dkr.ecr.us-east-1.amazonaws.com/external:latest'
        dockerImage = ''
        AWS_REGION = 'us-east-1'
        EKS_CLUSTER_NAME = 'devops'
    }
    stages {
        stage('Run the tests') {
            agent {
                docker { 
                    image 'node:14-alpine'
                    args '-e HOME=/tmp -e NPM_CONFIG_PREFIX=/tmp/.npm'
                    reuseNode true
                }
            }
            steps {
                echo 'Retrieve source from github. run npm install and npm test' 
            }
        }
        stage('Building image') {
            steps{
                script {
                    echo 'build the image' 
                }
            }
        }
        stage('Push Image') {
            steps{
                script {
                    echo 'push the image to docker hub' 
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
            steps{
                sh "docker rmi $imageName:latest"
                sh "docker rmi $imageName:$BUILD_NUMBER"
            }
        }
    }
}