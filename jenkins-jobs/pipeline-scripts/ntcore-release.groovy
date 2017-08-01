stage('build') {
    def builds = [:]
    builds['linux'] = {
        node('linux') {
            git poll: true, url:'https://github.com/wpilibsuite/ntcore.git'
            sh './gradlew clean :native:ntcore:build :native:wpiutil:build ntcoreSourceZip wpiutilSourceZip ' +
                    '-PreleaseType=OFFICIAL -PjenkinsBuild'
            stash includes: 'native/*/build/libs/**/*.jar, native/*/build/**/*.zip, build/*.zip, build/*.txt', name: 'linux'
        }
    }
    builds['mac'] = {
        node('mac') {
            git poll: true, url: 'https://github.com/wpilibsuite/ntcore.git'
            sh './gradlew clean :native:ntcore:build :native:wpiutil:build ntcoreSourceZip wpiutilSourceZip ' +
                    '-PreleaseType=OFFICIAL -PjenkinsBuild'
            stash includes: 'native/*/build/libs/**/*.jar, native/*/build/**/*.zip, build/*.zip, build/*.txt', name: 'mac'
        }
    }
    builds['windows'] = {
        node('windows') {
            git poll: true, url: 'https://github.com/wpilibsuite/ntcore.git'
            bat '.\\gradlew.bat clean :native:ntcore:build :native:wpiutil:build ntcoreSourceZip wpiutilSourceZip ' +
                    '-PreleaseType=OFFICIAL -PjenkinsBuild -PwithoutTests'
            stash includes: 'native/*/build/libs/**/*.jar, native/*/build/**/*.zip, build/*.zip, build/*.txt', name: 'windows'
        }
    }
    builds['arm'] = {
        node {
            ws("workspace/${env.JOB_NAME}/arm") {
                git poll: true, url: 'https://github.com/wpilibsuite/ntcore.git'
                sh './gradlew clean :arm:wpiutil:build :arm:ntcore:build -PreleaseType=OFFICIAL -PjenkinsBuild'
                stash includes: 'arm/*/build/libs/**/*.jar, arm/ntcore/build/ntcore-arm.zip, ' +
                        'arm/wpiutil/build/wpiutil-arm.zip', name: 'arm'
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
            dir('uberjar/products') {
                unstash 'linux'
                unstash 'mac'
                unstash 'windows'
                unstash 'arm'
            }
            sh 'chmod +x ./uberjar/gradlew'
            sh 'cd ./uberjar && ./gradlew clean publish -Prepo=release'
            archiveArtifacts 'uberjar/build/*.zip, uberjar/build/*.jar'
        }
    }
}
