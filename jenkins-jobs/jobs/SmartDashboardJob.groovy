def basePath = 'SmartDashboard'
folder(basePath) {
    description 'Jobs that build SmartDashboard.'
}

def prJob = job("$basePath/SmartDashboard - PR") {
    scm {
        git {
            remote {
                url('https://github.com/333fred/SmartDashboard.git')
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

def developmentJob = job("$basePath/SmartDashboard - Development") {
    triggers {
        scm('H/15 * * * *')
    }
}

setupGit(developmentJob)
setupProperties(developmentJob)
setupBuildSteps(developmentJob, true)

def releaseJob = job("$basePath/SmartDashboard - Release")

setupGit(releaseJob)
setupProperties(releaseJob)
setupBuildSteps(releaseJob, true, ['releaseType=OFFICIAL'])

def setupGit(job) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/333fred/SmartDashboard.git')
                    branch('*/gradle-version-generation')
                }
            }
        }
    }
}

def setupProperties(job) {
    job.with {
        // Note: The pull request builder plugin will fail without this property set.
        properties {
            githubProjectUrl('https://github.com/333fred/SmartDashboard')
        }
    }
}

def setupBuildSteps(job, usePublish, properties = null) {
    job.with {
        steps {
            gradle {
                tasks('clean')
                tasks(usePublish ? 'publish' : 'build')
                if (properties != null) {
                    properties.each { prop ->
                        switches("-P$prop")
                    }
                }
            }
        }
    }
}