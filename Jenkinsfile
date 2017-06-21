node {
    deleteDir()
    sh 'sudo make cleanall'
    checkout scm
    sh 'git submodule update --init' 
    stage ('build') {
        sh 'gradle clean build'
    }
    stage ('docker image') {
        sh 'make buildall'
    }
    stage ('docker image push') {
        withCredentials([usernamePassword(credentialsId: 'a9bc53ba-716c-45de-9d74-dd5d003f83c3', passwordVariable: 'DOCKER_PASSWD', usernameVariable: 'DOCKER_USER')]) {
            sh 'docker login -u $DOCKER_USER -p $DOCKER_PASSWD $DOCKER_REGISTRY'
            sh 'make pushall'
        }
    }
    stage('system tests') {
        withCredentials([usernamePassword(credentialsId: '8957fba6-7473-40f6-8593-efefa9e42251', passwordVariable: 'OPENSHIFT_PASSWD', usernameVariable: 'OPENSHIFT_USER')]) {
            withEnv(['SCRIPTS=https://raw.githubusercontent.com/EnMasseProject/travis-scripts/master']) {
                sh 'rm -rf systemtests && git clone https://github.com/EnMasseProject/systemtests.git'
                git url: 'https://github.com/redhat-maas-test/enmasse.git'

//                sh 'export OPENSHIFT_PROJECT=`echo $JOB_NAME | tr / -`; curl -s ${SCRIPTS}/run-tests.sh | bash /dev/stdin "" enmasse/install'
            }
        }
    }
    stage('docker snapshot') {
        sh 'make snapshotall'
    }
    deleteDir()
}
