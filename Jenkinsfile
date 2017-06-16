node {
    checkout scm
    sh 'git submodule update --init' 
    stage ('build') {
        sh 'gradle clean build'
    }
    stage ('docker images') {
        sh 'make'
    }
    environment {
        SCRIPTS = https://raw.githubusercontent.com/EnMasseProject/travis-scripts/master
    }
    stage('system tests') {
        sh 'curl -s ${SCRIPTS}/setup-tests.sh' 
    }
}
