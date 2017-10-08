def basePath = 'Documentation'
folder(basePath)

def prJob = job("$basePath/Documentation - PR") {
    scm {
        git {
            remote {
                url('https://github.com/wpilibsuite/DocumentationBuilder.git')
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


def developmentJob = job("$basePath/Documentation - Development") {
    triggers {
        scm('H/15 * * * *')
    }
}

setupGit(developmentJob)
setupProperties(developmentJob)
setupBuildSteps(developmentJob, false)

def releaseJob = job("$basePath/Documentation - Release")

setupGit(releaseJob)
setupProperties(releaseJob)
setupBuildSteps(releaseJob, true, ['releaseType=OFFICIAL'])

def setupGit(job) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/DocumentationBuilder.git')
                    branch('*/master')
                }
            }
        }
    }
}

def setupProperties(job, withParams = true) {
    job.with {
        // Note: The pull request builder plugin will fail without this property set.
        properties {
            githubProjectUrl('https://github.com/wpilibsuite/DocumentationBuilder')
        }
        if (withParams) {
            parameters {
                stringParam('docsLocation', "${System.getProperty('user.home')}/releases/development/docs/")
            }
        }
    }
}

def setupBuildSteps(job, doPublish, properties = null) {
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
            if (doPublish) {
                shell('rm -rf $docsLocation/cpp/ && mkdir -p $docsLocation/cpp/ && cp -r ./build/docs/doxygen/html/* $docsLocation/cpp/')
                shell('rm -rf $docsLocation/java/ && mkdir -p $docsLocation/java/ && cp -r ./build/docs/javadoc/* $docsLocation/java/')
            }
        }
        publishers {
            archiveArtifacts {
                pattern('build/*Docs.zip')
            }
        }
    }
}
