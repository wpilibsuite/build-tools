def basePath = 'WPILib'
folder(basePath)

['Windows', 'Linux', 'Mac'].each { platform ->
    def prJob = job("$basePath/WPILib $platform - PR") {
        label(platform.toLowerCase())
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                switches('-PjenkinsBuild -Pskiponlyathena -PreleaseBuild -PbuildAll --console=plain --info  --continue --rerun-tasks --stacktrace --refresh-dependencies')
            }
        }
    }
    setupProperties(prJob)
    setupPrJob(prJob, platform)
}

def athenaPrJob = job("$basePath/WPILib - PR Athena")
setupPrJob(athenaPrJob, 'Athena')
setupProperties(athenaPrJob)
setupBuildSteps(athenaPrJob, false, ['releaseBuild', 'onlylinuxathena'])

def developmentJob = pipelineJob("$basePath/WPILib - Development") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/allwpilib-development.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/allwpilib-development.groovy'))
            }
            sandbox()
        }
    }
    triggers {
        scm('H/15 * * * *')
    }
}

setupProperties(developmentJob)

def releaseJob = pipelineJob("$basePath/WPILib - Release") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/allwpilib-release.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/allwpilib-release.groovy'))
            }
            sandbox()
        }
    }
}

setupProperties(releaseJob)

// Allow anyone to release the mutex by running a job
def mutexJob = job("$basePath/Release Mutex") {
    steps {
        shell('ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -t admin@roborio-190-frc.local /usr/local/frc/bin/teststand give --name=`whoami`')
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

setupProperties(mutexJob)

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
                switches('-PjenkinsBuild --console=plain --continue --rerun-tasks --stacktrace --info')
                if (properties != null) {
                    properties.each { prop ->
                        switches("-P$prop")
                    }
                }
            }
            shell('cd test-scripts && chmod +x *.sh && ./jenkins-run-tests-get-results.sh')
        }
        publishers {
            archiveJunit('test-reports/*.xml')
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
