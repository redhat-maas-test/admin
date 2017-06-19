node {
    checkout scm
    sh 'git submodule update --init' 
    stage ('build') {
        sh 'gradle clean build'
    }
    stage ('docker images') {
        sh 'make'
    }
}
