def basePath = 'OpenCV'
folder(basePath)

['Linux', 'Mac'].each { platform ->
    def prJob = job("$basePath/OpenCV $platform - PR") {
        label(platform.toLowerCase())
        steps {
            shell('rm -rf build buildShared buildDebug buildSharedDebug')
            shell('./gradlew clean build -PjenkinsBuild --console=plain --stacktrace')
        }
    }
    setupProperties(prJob)
    setupPrJob(prJob, platform)
}

['Windows32'].each { platform ->
    def prJob = job("$basePath/OpenCV $platform - PR") {
        label('windows')
        steps {
            batchFile('del /s /q build buildShared buildDebug buildSharedDebug')
            batchFile('call "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\VC\\Auxiliary\\Build\\vcvars64.bat" && .\\gradlew.bat  clean build -PjenkinsBuild -Pplatform=windows-x86 --console=plain --stacktrace')
        }
    }
    setupProperties(prJob)
    setupPrJob(prJob, platform)
}

['Windows64'].each { platform ->
    def prJob = job("$basePath/OpenCV $platform - PR") {
        label('windows')
        steps {
            batchFile('del /s /q build buildShared buildDebug buildSharedDebug')
            batchFile('call "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\VC\\Auxiliary\\Build\\vcvars64.bat" && .\\gradlew.bat  clean build -PjenkinsBuild --console=plain --stacktrace')
        }
    }
    setupProperties(prJob)
    setupPrJob(prJob, platform)
}

['Athena', 'Raspbian'].each { platform ->
    def prJob = job("$basePath/OpenCV $platform - PR") {
        steps {
            shell('rm -rf build buildShared buildDebug buildSharedDebug')
            shell("./gradlew clean build -PjenkinsBuild -Pplatform=linux-${platform.toLowerCase()} --console=plain --stacktrace")
        }
    }
    setupProperties(prJob)
    setupPrJob(prJob, platform)
}

def releaseJob = pipelineJob("$basePath/OpenCV - Release") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/opencv-release.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/opencv-release.groovy'))
            }
            sandbox()
        }
    }
}

setupProperties(releaseJob)

def setupGit(job) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/thirdparty-opencv.git')
                    branch('*/master')
                }
            }
        }
    }
}

def setupProperties(job) {
    job.with {
        // Note: the pull request builder plugin will fail without this property set.
        properties {
            githubProjectUrl('https://github.com/wpilibsuite/thirdparty-opencv')
        }
        logRotator {
            numToKeep(50)
            artifactNumToKeep(10)
        }
    }
}

def setupPrJob(job, prContext) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/thirdparty-opencv.git')
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                }
                // This is purposefully not a GString. This is a jenkins environment
                // variable, not a groovy variable
                branch('${sha1}')
                extensions {
                    submoduleOptions {
                        disable false
                    }
                }
            }
        }
        triggers {
            githubPullRequest {
                admins(['333fred', 'PeterJohnson', 'bradamiller', 'Kevin-OConnor'])
                orgWhitelist('wpilibsuite')
                useGitHubHooks()
                extensions {
                    commitStatus {
                        context("frcjenkins - $prContext")
                    }
                }
            }
        }
    }
}
