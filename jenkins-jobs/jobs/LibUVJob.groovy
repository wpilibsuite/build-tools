def basePath = 'LibUV'
folder(basePath)

['Mac', 'Linux'].each { platform ->
    def prJob = job("$basePath/libuv $platform - PR") {
        label(platform.toLowerCase())
        steps {
            shell('git submodule update --init --recursive')
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
    def prJob = job("$basePath/libuv $platform - PR") {
        label(platform.toLowerCase())
        steps {
            bat('git submodule update --init --recursive')
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

def armPrJob = job("$basePath/libuv ARM - PR") {
    steps {
        shell('git submodule update --init --recursive')
        gradle {
            tasks('clean')
            tasks('build')
            switches('-PjenkinsBuild -PonlyAthena -PreleaseBuild -PbuildAll --continue --console=plain --stacktrace --refresh-dependencies')
        }
    }
}
setupProperties(armPrJob)
setupPrJob(armPrJob, 'ARM')

def releaseJob = pipelineJob("$basePath/libuv - Release") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/libuv-release.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/libuv-release.groovy'))
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
            githubProjectUrl('https://github.com/wpilibsuite/thirdparty-uvw')
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
                    url('https://github.com/wpilibsuite/thirdparty-uvw.git')
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                }
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
                        context("frcjenkins - $name")
                    }
                }
            }
        }
    }
}
