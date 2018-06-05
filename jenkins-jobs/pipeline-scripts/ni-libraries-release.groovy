stage('build') {
    def builds = [:]
    builds['linux'] = {
        node('linux') {
            git poll: true, url: 'https://github.com/wpilibsuite/ni-libraries.git'
            sh 'git submodule update --init --recursive'
            sh './gradlew clean build --console=plain --stacktrace --refresh-dependencies'
            stash includes: '**/allOutputs/*', name: 'linux'
        }
    }

    parallel builds
}

stage('combine') {
    node {
        ws("workspace/${env.JOB_NAME}/combine") {
            git poll: false, url: 'https://github.com/wpilibsuite/build-tools.git'
            sh 'git clean -xfd'
            dir('combiner/products') {
                unstash 'linux'
            }
            sh 'chmod +x ./combiner/gradlew'
            sh 'cd ./combiner && ./gradlew publish -Pthirdparty'
            archiveArtifacts 'combiner/products/**/*.zip, combiner/products/**/*.jar, combiner/outputs/**/*.*'
        }
    }
}
