def basePath = 'WPILib'
folder(basePath)

def prJob = job("$basePath/WPILib - PR") {
    scm {
        git {
            remote {
                url('https://github.com/333fred/allwpilib.git')
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

def developmentJob = job("$basePath/WPILib - Development") {
    triggers {
        scm('H/15 * * * *')
    }
}

setupProperties(developmentJob)
setupGit(developmentJob)
setupBuildSteps(developmentJob, true)

def releaseJob = job("$basePath/WPILib - Release")

setupProperties(releaseJob)
setupGit(releaseJob)
setupBuildSteps(releaseJob, true, ['releaseType=OFFICIAL'])

def setupGit(job) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/333fred/allwpilib.git')
                    branch('*/gradle-version-generation')
                }
            }
        }
    }
}

def setupProperties(job) {
    job.with {
        // Note: the pull request builder plugin will fail without this property set.
        properties {
            githubProjectUrl('https://github.com/333fred/allwpilib')
        }
    }
}

def setupBuildSteps(job, usePublish, properties = null) {
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