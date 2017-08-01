stage('build') {
    def builds = [:]
    builds['linux'] = {
        node('linux') {
            git poll: true, url: 'https://github.com/wpilibsuite/wpiutil.git'
            sh './gradlew clean build -PjenkinsBuild -PskipAthena -PreleaseBuild -PreleaseType=OFFICIAL'
            stash includes: 'build/libs/**/*.jar, build/outputs/**/*.*', name: 'linux'
        }
    }
    builds['mac'] = {
        node('mac') {
            git poll: true, url: 'https://github.com/wpilibsuite/wpiutil.git'
            sh './gradlew clean build -PjenkinsBuild -PskipAthena -PreleaseBuild -PreleaseType=OFFICIAL'
            stash includes: 'build/libs/**/*.jar, build/outputs/**/*.*', name: 'mac'
        }
    }
    builds['windows'] = {
        node('windows') {
            git poll: true, url: 'https://github.com/wpilibsuite/wpiutil.git'
            bat '.\\gradlew.bat  clean build -PjenkinsBuild -PskipAthena -PreleaseBuild -PreleaseType=OFFICIAL'
            stash includes: 'build/libs/**/*.jar, build/outputs/**/*.*', name: 'windows'
        }
    }
    builds['arm'] = {
        node {
            ws("workspace/${env.JOB_NAME}/arm") {
                git poll: true, url: 'https://github.com/wpilibsuite/wpiutil.git'
                sh './gradlew clean build -PjenkinsBuild -onlyAthena -PreleaseBuild -PreleaseType=OFFICIAL'
                stash includes: 'build/libs/**/*.jar, build/outputs/**/*.*', name: 'arm'
            }
        }
    }

    parallel builds
}

stage('combine') {
    node {
        ws("workspace/${env.JOB_NAME}/combine") {
            git poll: false, url: 'https://github.com/ThadHouse/JenkinsCombiner.git'
            sh 'git clean -xfd'
            dir('products') {
                unstash 'linux'
                unstash 'mac'
                unstash 'windows'
                unstash 'arm'
            }
            sh 'chmod +x ./gradlew'
            sh './gradlew publish -Pwpiutil -Prepo=release'
            archiveArtifacts 'products/*.zip, products/*.jar'
        }
    }
}
