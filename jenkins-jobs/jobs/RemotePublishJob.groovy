def basePath = 'remote-publish'
folder(basePath)

def runJob = job("$basePath/TriggeredRun")
setupGit(runJob)
setupBuildSteps(runJob)

def setupGit(job) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/RemotePublish.git')
                    branch('*/master')
                }
            }
        }
    }
}

def setupBuildSteps(job) {
    job.with {
        steps {
            gradle {
                tasks('build')
            }
        }
    }
}
