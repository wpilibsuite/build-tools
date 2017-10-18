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

setupProperties(prJob, false, 'pr')
setupBuildSteps(prJob, false, false)


def developmentJob = job("$basePath/Documentation - Development") {
    triggers {
        scm('H/15 * * * *')
    }
}

setupGit(developmentJob)
setupProperties(developmentJob, true, 'development')
setupBuildSteps(developmentJob, false, true)

def releaseJob = job("$basePath/Documentation - Release")

setupGit(releaseJob)
setupProperties(releaseJob, true, 'release')
setupBuildSteps(releaseJob, true, true, ['releaseType=OFFICIAL'])

def betaJob = job("$basePath/Documentation - Beta")

setupGit(betaJob)
setupProperties(betaJob, true, 'beta')
setupBuildSteps(betaJob, true, true, ['releaseType=OFFICIAL'])

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

def setupProperties(job, withParams, paramsRepo) {
    job.with {
        // Note: The pull request builder plugin will fail without this property set.
        properties {
            githubProjectUrl('https://github.com/wpilibsuite/DocumentationBuilder')
        }
        if (withParams) {
            parameters {
                stringParam('docsLocation', "${System.getProperty('user.home')}/releases/${paramsRepo}/docs/")
            }
        }
    }
}

def setupBuildSteps(job, doWebPublish, doMavenPublish,  properties = null) {
    job.with {
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                if (doMavenPublish) {
                    tasks('publish')
                }
                switches('-PjenkinsBuild')
                if (properties != null) {
                    properties.each { prop ->
                        switches("-P$prop")
                    }
                }
            }
            if (doWebPublish) {
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
