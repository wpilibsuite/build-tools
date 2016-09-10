def releaseFolder = "${System.getProperty('user.home')}/releases/release/eclipse/"
def releaseJob = job('Eclipse Plugins') {
    scm {
        git('https://github.com/wpilibsuite/EclipsePlugins.git')
    }
    triggers {
        scm('H/15 * * * *')
    }
}

setupBuildSteps(releaseJob)
setupPublish(releaseJob, releaseFolder)

def prJob = job('Eclipse Plugins PR') {
    scm {
        git('https://github.com/333fred/EclipsePlugins.git')
    }
    triggers {
        githubPullRequest {
            admins(['333fred', 'PeterJohnson', 'bradamiller', 'Kevin-OConnor'])
            orgWhitelist('wpilibsuite')
            triggerPhrase('OK to test')
            useGitHubHooks()
        }
    }
}

setupBuildSteps(prJob)

def setupBuildSteps(job) {
    job.with {
        steps {
            gradle('clean')
            gradle('build')
        }
    }
}

def setupPublish(job, location) {
    job.with {
        publishers {
            postBuildScripts {
                steps {
                    shell("rm -rf $location && " +
                          "mkdir -p $location && " +
                          "cp -r ./edu.wpi.first.wpilib.plugins.updatesite/target/site/* $location")
                }
            }
        }
    }
}