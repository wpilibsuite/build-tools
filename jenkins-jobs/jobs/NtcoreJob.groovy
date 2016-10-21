def basePath = 'ntcore'
folder(basePath)

['Windows', 'Mac', 'Linux'].each { platform ->
    def prJob = job("$basePath/ntcore $platform - PR") {
        label(platform.toLowerCase())
        steps {
            gradle {
                tasks('clean')
                tasks(':native:wpiutil:build')
                tasks(':native:ntcore:build')
            }
        }
    }
    setupProperties(prJob)
    setupPrJob(prJob)
}

def armPrJob = job("$basePath/ntcore ARM - PR") {
    steps {
        gradle {
            tasks('clean')
            tasks(':arm:wpiutil:build')
            tasks(':arm:ntcore:build')
        }
    }
}
setupProperties(armPrJob)
setupPrJob(armPrJob)

def developmentJob = pipelineJob("$basePath/ntcore - Development") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/ntcore-development.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/ntcore-development.groovy'))
            }
            sandbox()
        }
    }
    triggers {
        scm('H/15 * * * *')
    }
}

setupProperties(developmentJob)

def releaseJob = pipelineJob("$basePath/ntcore - Release") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/ntcore-release.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/ntcore-release.groovy'))
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

def setupPrJob(job) {
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
            }
        }
    }
}
