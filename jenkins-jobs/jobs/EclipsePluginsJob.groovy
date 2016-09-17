def releaseFolder = "${System.getProperty('user.home')}/releases/release/eclipse/"
def releaseJob = job('Eclipse Plugins') {
    scm {
        git {
            remote {
                url('https://github.com/333fred/EclipsePlugins.git')
            }
            branch('gradleBuildGeneration')
        }
    }
    triggers {
        scm('H/15 * * * *')
    }
}

setupProperties(releaseJob)
setupBuildSteps(releaseJob, ['releaseType=official'])
setupPublish(releaseJob, releaseFolder)

def prJob = job('Eclipse Plugins PR') {
    scm {
        git {
            remote {
                url('https://github.com/333fred/EclipsePlugins.git')
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

setupProperties(prJob)
setupBuildSteps(prJob)

def setupProperties(job) {
    job.with {
        // Note: The pull request builder plugin will fail without this property set.
        properties {
            githubProjectUrl('https://github.com/333fred/EclipsePlugins')
        }
    }
}

def setupBuildSteps(job, properties = null) {
    job.with {
        steps {
            gradle {
                tasks('clean')
                tasks('updateDependencies')
                tasks('build')
                if (properties != null) {
                    properties.each { prop ->
                        switches("-P$prop")
                    }
                }
            }
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