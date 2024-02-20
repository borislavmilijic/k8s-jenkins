pipeline {
  agent {
    kubernetes {
          inheritFrom 'kaniko maven python'
          /*use the templates needed
           possible options to inherit from (ask DevOps if you want add additional templates ):
           - kaniko (for building and pushing docker images)
           - maven
           - python
           - OR leave empty for default jenkins pod
          */
        }
    }
    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/ianmiell/simple-dockerfile.git'
            }
        }
        stage('Make Image') {
            environment {
                PATH = "/busybox:$PATH"
            }
            steps {
                // container to build docker images
                container('kaniko') {
                    // adapt the docker repository and if needed path to the Dockerfile
                    sh '''#!/busybox/sh
                    /kaniko/executor -f `pwd`/Dockerfile -c `pwd` --insecure --cache=true --destination=auroranexus.smartstream-stp.com:5000/test-image:0.0.1
                    '''
                }
                // container to build maven artifacts
                container('maven') {
                    echo 'Hello from maven container!'
                    sh '''mvn -v'''
                }
                // container to build python apps
                container('python') {
                    
                    sh '''python3 --version'''
                    echo 'Hello from python container!'
                }
            }
        }
    }
}

// example definition of PodTemplate definition from scratch
podTemplate(containers: [
    containerTemplate(name: 'golang', image: 'golang:1.8.0', ttyEnabled: true, command: 'cat')
  ]) {

    node(POD_LABEL) {
        stage('Get a Golang project') {
            git url: 'https://github.com/hashicorp/terraform.git'
            container('golang') {
                stage('Build a Go project') {
                    sh """
                    mkdir -p /go/src/github.com/hashicorp
                    ln -s `pwd` /go/src/github.com/hashicorp/terraform
                    cd /go/src/github.com/hashicorp/terraform && make core-dev
                    """
                }
            }
        }

    }
}

/* parallel container pipeline with custom yamls with different env variables*/
pipeline {
    agent none
    stages {        
        stage('Test'){
            parallel {
                
                stage("rook") {
                    
                    agent {
                        kubernetes {
                            yaml '''
                            spec:
                                containers:
                                - name: cloud-storage-service
                                  image: auroranexus.smartstream-stp.com:5000/pegasus-cloud-storage-service:0.1.0-SNAPSHOT
                                  command:
                                  - sleep
                                  args:
                                  - 99d
                                  env:
                                  - name: AWS_URL
                                    value: 'rookURL'
                            '''
                        }
                    }
                    
                    steps {
                        container('cloud-storage-service') {
                            sh 'env | grep AWS'
                        }
                    }
                }
                
                stage("minio") {
                    
                    agent {
                        kubernetes {
                            yaml '''
                            spec:
                                containers:
                                - name: cloud-storage-service
                                  image: auroranexus.smartstream-stp.com:5000/pegasus-cloud-storage-service:0.1.0-SNAPSHOT
                                  command:
                                  - sleep
                                  args:
                                  - 99d
                                  env:
                                  - name: AWS_URL
                                    value: 'minioURL'
                            '''
                        }
                    }
                    
                    steps {
                        container('cloud-storage-service') {
                            sh 'env | grep AWS'
                        }
                    }
                }

                stage("aws") {
                    
                    agent {
                        kubernetes {
                            yaml '''
                            spec:
                                containers:
                                - name: cloud-storage-service
                                  image: auroranexus.smartstream-stp.com:5000/pegasus-cloud-storage-service:0.1.0-SNAPSHOT
                                  command:
                                  - sleep
                                  args:
                                  - 99d
                                  env:
                                  - name: AWS_URL
                                    value: 'awsURL'
                            '''
                        }
                    }
                    
                    steps {
                        container('cloud-storage-service') {
                            sh 'env | grep AWS'
                        }
                    }
                }
            }
        }
    }
}