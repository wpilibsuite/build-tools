def basePath = 'WPILib'
folder(basePath)

['Windows', 'Linux'].each { platform ->
    def prJob = job("$basePath/WPILib $platform - PR") {
        label(platform.toLowerCase())
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                switches('-PjenkinsBuild -PskipAthena -PreleaseBuild -PbuildAll --console=plain --stacktrace --refresh-dependencies')
            }
        }
    }
    setupProperties(prJob, false)
    setupPrJob(prJob, platform)
}

def athenaPrJob = job("$basePath/WPILib - PR Athena")
setupPrJob(athenaPrJob, 'Athena')
setupProperties(athenaPrJob)
setupBuildSteps(athenaPrJob, false, ['-PonlyAthena', '-PreleaseBuild'])

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

// Allow anyone to release the mutex by running a job
def mutexJob = job("$basePath/Release Mutex") {
    steps {
        shell('ssh -t admin@roborio-190-frc.local /usr/local/frc/bin/teststand give --name=`whoami`')
    }

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
            orgWhitelist('wpilibsuite')
            useGitHubHooks()
            allowMembersOfWhitelistedOrgsAsAdmin()
            onlyTriggerPhrase()
            triggerPhrase('(?i).*release\\Wthe\\Wmutex.*')
            extensions {
                commitStatus {
                    context("frcjenkins - Mutex Release")
                }
            }
        }
    }
}

setupProperties(mutexJob, false)

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

def setupProperties(job, withParams = true) {
    job.with {
        // Note: the pull request builder plugin will fail without this property set.
        properties {
            githubProjectUrl('https://github.com/wpilibsuite/allwpilib')
        }
        if (withParams) {
            parameters {
                stringParam('docsLocation', "${System.getProperty('user.home')}/releases/development/docs/")
            }
        }
    }
}

def setupBuildSteps(job, usePublish, properties = null, jobName = null) {
    job.with {
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                switches('-PjenkinsBuild --console=plain --stacktrace')
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
                shell('rm -rf $docsLocation/java/ && mkdir -p $docsLocation/java/ && cp -r ./wpilibj/build/docs/javadoc/* $docsLocation/java/')
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
