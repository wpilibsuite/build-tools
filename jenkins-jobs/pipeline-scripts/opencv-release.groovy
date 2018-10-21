stage('build') {
    def builds = [:]
    builds['linux'] = {
        node('linux') {
            git poll: true, url: 'https://github.com/wpilibsuite/thirdparty-opencv.git'
            sh 'git submodule update --init --recursive'
            sh 'rm -rf build buildShared'
            sh './gradlew clean build -PjenkinsBuild --console=plain --stacktrace'
            stash includes: '**/allOutputs/*', name: 'linux'
        }
    }
    builds['mac'] = {
        node('mac') {
            git poll: true, url: 'https://github.com/wpilibsuite/thirdparty-opencv.git'
            sh 'git submodule update --init --recursive'
            sh 'rm -rf build buildShared'
            sh './gradlew clean build -PjenkinsBuild --console=plain --stacktrace'
            stash includes: '**/allOutputs/*', name: 'mac'
        }
    }
    builds['windows32'] = {
        node('windows') {
            git poll: true, url: 'https://github.com/wpilibsuite/thirdparty-opencv.git'
            bat 'git submodule update --init --recursive'
            bat 'del /s /q build buildShared'
            bat 'call "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\VC\\Auxiliary\\Build\\vcvars32.bat" && .\\gradlew.bat  clean build -PjenkinsBuild -Pplatform=windows-x86 --console=plain --stacktrace'
            stash includes: '**/allOutputs/*', name: 'windows32'
        }
    }
    builds['windows64'] = {
        node('windows') {
            git poll: true, url: 'https://github.com/wpilibsuite/thirdparty-opencv.git'
            bat 'git submodule update --init --recursive'
            bat 'del /s /q build buildShared'
            bat 'call "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\VC\\Auxiliary\\Build\\vcvars64.bat" && .\\gradlew.bat  clean build -PjenkinsBuild --console=plain --stacktrace'
            stash includes: '**/allOutputs/*', name: 'windows64'
        }
    }
    builds['arm'] = {
        node {
            ws("workspace/${env.JOB_NAME}/arm") {
                git poll: true, url: 'https://github.com/wpilibsuite/thirdparty-opencv.git'
                sh 'git submodule update --init --recursive'
                sh 'rm -rf build buildShared'
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
                unstash 'windows32'
                unstash 'windows64'
                unstash 'arm'
            }
            sh 'chmod +x ./combiner/gradlew'
            sh 'cd ./combiner && ./gradlew publish -Pthirdparty'
            archiveArtifacts 'combiner/products/**/*.zip, combiner/products/**/*.jar, combiner/outputs/**/*.*'
        }
    }
}
