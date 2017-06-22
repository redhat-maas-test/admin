node {
    checkout scm
    sh 'git submodule update --init' 
    stage ('build') {
        sh 'gradle clean check -i'
        junit '**/build/test-results/TEST-*.xml'
    }
    stage ('docker image') {
        sh 'make TMPDIR=`mktemp -d`'
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
                sh 'rm -rf enmasse && git clone https://github.com/redhat-maas-test/enmasse.git'
                sh 'export OPENSHIFT_PROJECT=$BUILD_TAG; curl -s ${SCRIPTS}/run-tests.sh | bash /dev/stdin "" enmasse/install jboss-amqmaas-1-tech-preview/amqmaas10-addresscontroller-openshift jboss-amqmaas-1-tech-preview/amqmaas10-configserv-openshift jboss-amqmaas-1-tech-preview/amqmaas10-queuescheduler-openshift'
                junit 'systemtests/target/surefire-reports/TEST-*.xml'
            }
        }
    }
    stage('docker snapshot') {
        sh 'make snapshotall'
    }
    deleteDir()
}
