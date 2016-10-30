def basePath = 'Eclipse Plugins'
folder(basePath)

setupReleaseBuild('Release', basePath)
setupReleaseBuild('Stable', basePath)
setupReleaseBuild('Beta', basePath)

def developmentFolder = "${System.getProperty('user.home')}/releases/development/eclipse/"
def developmentJob = job("$basePath/Eclipse Plugins - Development") {
    scm {
        git {
            remote {
                url('https://github.com/wpilibsuite/EclipsePlugins.git')
            }
            branch('*/master')
        }
    }
    triggers {
        scm('H/15 * * * *')
    }
}

setupProperties(developmentJob)
setupBuildSteps(developmentJob)
setupPublish(developmentJob, developmentFolder)

def prJob = job("$basePath/Eclipse Plugins PR") {
    scm {
        git {
            remote {
                url('https://github.com/wpilibsuite/EclipsePlugins.git')
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
            githubProjectUrl('https://github.com/wpilibsuite/EclipsePlugins')
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
                switches('-PjenkinsBuild')
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

def setupReleaseBuild(String type, String path) {
    def releaseFolder = "${System.getProperty('user.home')}/releases/${type.toLowerCase()}/eclipse/"
    def releaseJob = job("$path/Eclipse Plugins - $type") {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/EclipsePlugins.git')
                }
                branch('*/master')
            }
        }
    }

    setupProperties(releaseJob)
    setupBuildSteps(releaseJob, ['releaseType=OFFICIAL'])
    setupPublish(releaseJob, releaseFolder)
}
