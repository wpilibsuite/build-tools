def basePath = 'RobotBuilder'
folder(basePath)

def prJob = job("$basePath/RobotBuilder - PR") {
    scm {
        git {
            remote {
                url('https://github.com/wpilibsuite/RobotBuilder.git')
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
setupXvfb(prJob)
setupBuildSteps(prJob, false)

def developmentJob = job("$basePath/RobotBuilder - Development") {
    triggers {
        scm('H/15 * * * *')
    }
    publishers {
        downstream('Eclipse Plugins/Eclipse Plugins - Development')
    }
}

setupGit(developmentJob)
setupProperties(developmentJob)
setupXvfb(prJob)
setupBuildSteps(developmentJob, true)

def releaseJob = job("$basePath/RobotBuilder - Release")

setupGit(releaseJob)
setupProperties(releaseJob)
setupXvfb(releaseJob)
setupBuildSteps(releaseJob, true, ['releaseType=OFFICIAL'])

def setupGit(job) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/RobotBuilder.git')
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
            githubProjectUrl('https://github.com/wpilibsuite/RobotBuilder')
        }
    }
}

def setupBuildSteps(job, usePublish, properties = null) {
    job.with {
        steps {
            gradle {
                tasks('clean')
                tasks(usePublish ? 'publish' : 'build')
                switches('-PjenkinsBuild')
                if (properties != null) {
                    properties.each { prop ->
                        switches("-P$prop")
                    }
                }
            }
        }
    }
}

def setupXvfb(job) {
    job.with {
        wrappers {
            xvfb('default')
        }
    }
}
