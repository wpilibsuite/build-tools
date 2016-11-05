def basePath = 'SmartDashboard'
folder(basePath)

def prJob = job("$basePath/SmartDashboard - PR") {
    scm {
        git {
            remote {
                url('https://github.com/wpilibsuite/SmartDashboard.git')
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
    publishers {
        downstream('Eclipse Plugins/Eclipse Plugins - Development')
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
                    url('https://github.com/wpilibsuite/SmartDashboard.git')
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
            githubProjectUrl('https://github.com/wpilibsuite/SmartDashboard')
        }
    }
}

def setupBuildSteps(job, usePublish, properties = null) {
    job.with {
        steps {
            gradle {
                rootBuildScriptDir('smartdashboard')
                tasks('clean')
                tasks('build')
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
                    pattern('smartdashboard/build/libs/smartdashboard-all.jar')
                }
            }
        }
    }
}
