stage('build') {
    def builds = [:]
    builds['linux'] = {
        node('linux') {
            git poll: true, url: 'https://github.com/wpilibsuite/thirdparty-opencv.git'
            sh './gradlew clean build -PjenkinsBuild --console=plain --stacktrace'
            stash includes: '**/allOutputs/*', name: 'linux'
        }
    }
    builds['mac'] = {
        node('mac') {
            git poll: true, url: 'https://github.com/wpilibsuite/thirdparty-opencv.git'
            sh './gradlew clean build -PjenkinsBuild --console=plain --stacktrace'
            stash includes: '**/allOutputs/*', name: 'mac'
        }
    }
    builds['windows'] = {
        node('windows') {
            git poll: true, url: 'https://github.com/wpilibsuite/thirdparty-opencv.git'
            bat '.\\gradlew.bat  clean build -PjenkinsBuild --console=plain --stacktrace'
            stash includes: '**/allOutputs/*', name: 'windows'
        }
    }
    builds['arm'] = {
        node {
            ws("workspace/${env.JOB_NAME}/arm") {
                git poll: true, url: 'https://github.com/wpilibsuite/thirdparty-opencv.git'
                sh './gradlew clean build -PjenkinsBuild -Pplatform=linux-athena --console=plain --stacktrace'
                stash includes: '**/allOutputs/*', name: 'arm'
            }
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
                unstash 'mac'
                unstash 'windows'
                unstash 'arm'
            }
            sh 'chmod +x ./combiner/gradlew'
            sh 'cd ./combiner && ./gradlew publish -Pthirdparty -Prepo=release'
            archiveArtifacts 'combiner/products/**/*.zip, combiner/products/**/*.jar, combiner/outputs/**/*.*'
        }
    }
}
