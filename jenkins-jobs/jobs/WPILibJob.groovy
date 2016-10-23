def basePath = 'WPILib'
folder(basePath)

def athenaPrJob = job("$basePath/WPILib - PR Athena")
setupPrJob(athenaPrJob, 'Athena')
setupProperties(athenaPrJob)
setupBuildSteps(athenaPrJob, false)

def simPrJob = job("$basePath/WPILib - PR Sim")
setupPrJob(simPrJob, 'Sim')
setupProperties(simPrJob)
setupBuildSteps(simPrJob, false, ['makeSim'], false)

def developmentJob = job("$basePath/WPILib - Development") {
    triggers {
        scm('H/15 * * * *')
    }
    publishers {
        downstream('Eclipse Plugins/Eclipse Plugins - Development', 'UNSTABLE')
    }
}

setupProperties(developmentJob)
setupGit(developmentJob)
setupBuildSteps(developmentJob, true, ['makeSim'])

def releaseJob = job("$basePath/WPILib - Release")

setupProperties(releaseJob)
setupGit(releaseJob)
setupBuildSteps(releaseJob, true, ['releaseType=OFFICIAL', 'makeSim'])

def setupGit(job) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/allwpilib.git')
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
            githubProjectUrl('https://github.com/wpilibsuite/allwpilib')
        }
    }
}

def setupBuildSteps(job, usePublish, properties = null, test = true) {
    job.with {
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                if (properties != null) {
                    properties.each { prop ->
                        switches("-P$prop")
                    }
                }
            }
            if (test)
                shell('cd test-scripts && chmod +x *.sh && ./jenkins-run-tests-get-results.sh')
            if (usePublish) {
                gradle {
                    tasks('publish')
                    if (properties != null) {
                        properties.each { prop ->
                            switches("-P$prop")
                        }
                    }
                }
            }
        }
        publishers {
            archiveJunit('test-reports/*.xml')
        }
    }
}

def setupPrJob(job, context) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/allwpilib.git')
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
                extensions {
                    commitStatus {
                        context("frcjenkins - $context")
                    }
                }
            }
        }
    }
}
