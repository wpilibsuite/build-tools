def basePath = 'ntcore2'
folder(basePath)

['Windows', 'Mac', 'Linux'].each { platform ->
    def prJob = job("$basePath/ntcore2 $platform - PR") {
        label(platform.toLowerCase())
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                switches('-PjenkinsBuild -PskipAthena -PreleaseBuild -PbuildAll --console=plain --stacktrace')
            }
        }
    }
    setupProperties(prJob)
    setupPrJob(prJob, platform)
}

def armPrJob = job("$basePath/ntcore2 ARM - PR") {
    steps {
        gradle {
            tasks('clean')
            tasks('build')
            switches('-PjenkinsBuild -PonlyAthena -PreleaseBuild -PbuildAll --console=plain --stacktrace')
        }
    }
}
setupProperties(armPrJob)
setupPrJob(armPrJob, 'ARM')

def developmentJob = pipelineJob("$basePath/ntcore2 - Development") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/ntcore2-development.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/ntcore2-development.groovy'))
            }
            sandbox()
        }
    }
    triggers {
        scm('H/15 * * * *')
    }
}

setupProperties(developmentJob)

def releaseJob = pipelineJob("$basePath/ntcore2 - Release") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/ntcore2-release.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/ntcore2-release.groovy'))
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
            githubProjectUrl('https://github.com/wpilibsuite/ntcore')
        }
    }
}

def setupPrJob(job, name) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/ntcore.git')
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
