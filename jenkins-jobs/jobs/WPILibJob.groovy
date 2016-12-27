def basePath = 'WPILib'
folder(basePath)

def athenaPrJob = job("$basePath/WPILib - PR Athena")
setupPrJob(athenaPrJob, 'Athena')
setupProperties(athenaPrJob)
setupBuildSteps(athenaPrJob, false)

def simPrJob = job("$basePath/WPILib - PR Sim")
setupPrJob(simPrJob, 'Sim')
setupProperties(simPrJob)
setupSimBuildSteps(simPrJob)

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
setupBuildSteps(developmentJob, true, ['makeSim'], 'development')

def releaseJob = job("$basePath/WPILib - Release")

setupProperties(releaseJob)
setupGit(releaseJob)
setupBuildSteps(releaseJob, true, ['releaseType=OFFICIAL', 'makeSim'], 'release')

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
        parameters {
            stringParam('docsLocation', '~/releases/development/docs/')
        }
    }
}

def setupBuildSteps(job, usePublish, properties = null, jobName = null) {
    job.with {
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                switches('-PjenkinsBuild')
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
                    switches('-PjenkinsBuild')
                    if (properties != null) {
                        properties.each { prop ->
                            switches("-P$prop")
                        }
                    }
                }
                shell('rm -rf $docsLocation/cpp/ && mkdir -p $docsLocation/cpp/ && cp -r ./wpilibc/build/docs/html/* $docsLocation/cpp/')
                shell('rm -rf $docsRoot/java/ && mkdir -p $docsRoot/java/ && cp -r ./wpilibj/build/docs/javadoc/* $docsLocation/java/')
            }
        }
        publishers {
            archiveJunit('test-reports/*.xml')
            if (usePublish) {
                archiveArtifacts {
                    pattern('**/build/**/*.zip')
                    pattern('**/build/**/*.jar')
                }
            }
        }
    }
}

def setupSimBuildSteps(job) {
    job.with {
        steps {
            gradle {
                tasks('clean')
                tasks('wpilibjSimJar')
                tasks('allcsim')
                switches('-PjenkinsBuild')
                switches('-PmakeSim')
            }
        }
    }
}

def setupPrJob(job, prContext) {
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
                        context("frcjenkins - $prContext")
                    }
                }
            }
        }
    }
}
