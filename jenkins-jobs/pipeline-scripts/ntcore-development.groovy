stage('build') {
    def builds = [:]
    builds['linux'] = {
        node('linux') {
            git poll: true, url: 'https://github.com/wpilibsuite/ntcore.git'
            sh './gradlew clean build -PjenkinsBuild -PskipAthena -PreleaseBuild -PbuildAll --console=plain --stacktrace'
            stash includes: 'build/outputs/**/*.*', name: 'linux'
        }
    }
    builds['mac'] = {
        node('mac') {
            git poll: true, url: 'https://github.com/wpilibsuite/ntcore.git'
            sh './gradlew clean build -PjenkinsBuild -PskipAthena -PreleaseBuild -PbuildAll --console=plain --stacktrace'
            stash includes: 'build/outputs/**/*.*', name: 'mac'
        }
    }
    builds['windows'] = {
        node('windows') {
            git poll: true, url: 'https://github.com/wpilibsuite/ntcore.git'
            bat '.\\gradlew.bat  clean build -PjenkinsBuild -PskipAthena -PreleaseBuild -PbuildAll --console=plain --stacktrace'
            stash includes: 'build/outputs/**/*.*', name: 'windows'
        }
    }
    builds['arm'] = {
        node {
            ws("workspace/${env.JOB_NAME}/arm") {
                git poll: true, url: 'https://github.com/wpilibsuite/ntcore.git'
                sh './gradlew clean build -PjenkinsBuild -PonlyAthena -PreleaseBuild -PbuildAll --console=plain --stacktrace'
                stash includes: 'build/libs/**/*.jar, build/outputs/**/*.*', name: 'arm'
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
            sh 'cd ./combiner && ./gradlew publish -Pntcore'
            archiveArtifacts 'combiner/products/**/*.zip, combiner/products/**/*.jar'
        }
    }
}
