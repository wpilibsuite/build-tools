def basePath = 'OpenCV'
folder(basePath)

['Windows', 'Linux', 'Mac'].each { platform ->
    def prJob = job("$basePath/OpenCV $platform - PR") {
        label(platform.toLowerCase())
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                switches('-PjenkinsBuild --console=plain --info  --continue --stacktrace')
            }
        }
    }
    setupProperties(prJob)
    setupPrJob(prJob, platform)
}

def athenaPrJob = job("$basePath/OpenCV - PR Athena")
setupPrJob(athenaPrJob, 'Athena')
setupProperties(athenaPrJob)
setupBuildSteps(athenaPrJob, false, ['platform=linux-athena'])

def developmentJob = pipelineJob("$basePath/OpenCV - Development") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/opencv-development.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/opencv-development.groovy'))
            }
            sandbox()
        }
    }
    triggers {
        scm('H/15 * * * *')
    }
}

setupProperties(developmentJob)

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

def setupBuildSteps(job, usePublish, properties = null, jobName = null) {
    job.with {
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                switches('-PjenkinsBuild --console=plain --continue --stacktrace --info')
                if (properties != null) {
                    properties.each { prop ->
                        switches("-P$prop")
                    }
                }
            }
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
