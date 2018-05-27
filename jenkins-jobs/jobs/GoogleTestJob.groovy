def basePath = 'GoogleTest'
folder(basePath)

['Mac', 'Linux'].each { platform ->
    def prJob = job("$basePath/googletest $platform - PR") {
        label(platform.toLowerCase())
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                switches('-PjenkinsBuild -PskipAthena -PreleaseBuild -PbuildAll --continue --console=plain --stacktrace --refresh-dependencies')
                configure {
                    passAllAsProjectProperties = false
                    passAllAsSystemProperties = false
                }
            }
        }
    }
    setupProperties(prJob)
    setupPrJob(prJob, platform)
}

['Windows'].each { platform ->
    def prJob = job("$basePath/googletest $platform - PR") {
        label(platform.toLowerCase())
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                switches('-PjenkinsBuild -PskipAthena -PreleaseBuild -PbuildAll --continue --console=plain --stacktrace --refresh-dependencies')
                configure {
                    passAllAsProjectProperties = false
                    passAllAsSystemProperties = false
                }
            }
        }
    }
    setupProperties(prJob)
    setupPrJob(prJob, platform)
}

def armPrJob = job("$basePath/googletest ARM - PR") {
    steps {
        gradle {
            tasks('clean')
            tasks('build')
            switches('-PjenkinsBuild -PonlyAthena -PreleaseBuild -PbuildAll --continue --console=plain --stacktrace --refresh-dependencies')
        }
    }
}
setupProperties(armPrJob)
setupPrJob(armPrJob, 'ARM')

def releaseJob = pipelineJob("$basePath/googletest - Release") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/googletest-release.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/googletest-release.groovy'))
            }
            sandbox()
        }
    }
}

setupProperties(releaseJob)

def setupProperties(job) {
    job.with {
        // Note: the pull request builder plugin will fail without this property set.
        properties {
            githubProjectUrl('https://github.com/wpilibsuite/thirdparty-googletest')
        }
        logRotator {
            numToKeep(50)
            artifactNumToKeep(10)
        }
    }
}

def setupPrJob(job, name) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/thirdparty-googletest.git')
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                }
                branch('${sha1}')
                extensions {
                    submoduleOptions {
                        disable false
                    }
                }
            }
        }
        triggers {
            githubPullRequest {
                admins(['333fred', 'PeterJohnson', 'bradamiller', 'Kevin-OConnor'])
                orgWhitelist('wpilibsuite')
                useGitHubHooks()
                extensions {
                    commitStatus {
                        context("frcjenkins - $name")
                    }
                }
            }
        }
    }
}
