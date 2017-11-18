def basePath = 'Shuffleboard'
folder(basePath)

def prJob = job("$basePath/Shuffleboard - PR") {
    scm {
        git {
            remote {
                url('https://github.com/wpilibsuite/shuffleboard.git')
                refspec('+refs/pull/*:refs/remotes/origin/pr/*')
            }
            // This is purposefully not a GString. This is a jenkins environment
            // variable, not a groovy variable
            branch('${sha1}')
        }
    }
    triggers {
        githubPullRequest {
            admins(['333fred', 'PeterJohnson', 'bradamiller', 'Kevin-OConnor'])
            orgWhitelist('wpilibsuite')
            useGitHubHooks()
        }
    }
}

setupProperties(prJob)
setupBuildSteps(prJob, false)

def developmentJob = job("$basePath/Shuffleboard - Development") {
    triggers {
        scm('H/15 * * * *')
    }
    publishers {
        downstream('Eclipse Plugins/Eclipse Plugins - Development')
    }
}

setupGit(developmentJob)
setupProperties(developmentJob)
setupBuildSteps(developmentJob, true)

def releaseJob = job("$basePath/Shuffleboard - Release")

setupGit(releaseJob)
setupProperties(releaseJob)
setupBuildSteps(releaseJob, true, ['releaseType=OFFICIAL'])

def setupGit(job) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/shuffleboard.git')
                    branch('*/master')
                }
            }
        }
    }
}

def setupProperties(job) {
    job.with {
        // Note: The pull request builder plugin will fail without this property set.
        properties {
            githubProjectUrl('https://github.com/wpilibsuite/shuffleboard')
        }
    }
}

def setupBuildSteps(job, usePublish, properties = null) {
    job.with {
        wrappers {
            xvfb('default') {
                screen('1920x1080x24')
            }
        }
        steps {
            gradle {
                tasks('clean')
                tasks('check')
                if (usePublish) tasks('publish')
                switches('-PjenkinsBuild')
                if (properties != null) {
                    properties.each { prop ->
                        switches("-P$prop")
                    }
                }
            }
        }
        if (usePublish) {
            publishers {
                archiveArtifacts {
                    pattern('app/build/libs/*.jar')
                }
            }
        }
    }
}
