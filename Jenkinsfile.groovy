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
        stage('Install Docker') {
            steps {
                echo 'Installing Docker on Jenkins agent'
                sh '''
                    if ! command -v docker &> /dev/null
                    then
                        echo "Docker not found, installing..."
                        # For Ubuntu/Debian agents
                        if [ -f /etc/debian_version ]; then
                            sudo apt-get update
                            sudo apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release
                            curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
                            echo \
                              "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
                              $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
                            sudo apt-get update
                            sudo apt-get install -y docker-ce docker-ce-cli containerd.io
                        # For Amazon Linux agents
                        elif [ -f /etc/system-release ] && grep -q "Amazon Linux" /etc/system-release; then
                            sudo yum update -y
                            sudo amazon-linux-extras install docker -y
                            sudo service docker start
                        else
                            echo "Unsupported OS. Please install Docker manually."
                            exit 1
                        fi
                        sudo usermod -aG docker $(whoami)
                    else
                        echo "Docker is already installed"
                    fi
                '''
            }
        }
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