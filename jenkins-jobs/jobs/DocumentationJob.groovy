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
        logRotator {
            numToKeep(50)
            artifactNumToKeep(10)
        }
    }
}

/**
 * Generates a shell command for nuking a docs directory and copying the most up-to-date docs
 * into it. This assumes that the documentation files are located in "./build/docs".
 *
 * @param artifact the artifact to copy the docs for (eg "java", "cpp", "shuffleboard/api")
 * @param docFormat the format string that matches the documentation files to copy, relative to the
 *                  artifact's root documentation directory, eg "javadoc/*", "doxygen/html/*"
 */
def docCopyCommand(artifact, docFormat) {
    def dst = "$docsLocation/$artifact/"
    return "rm -rf $dst && mkdir -p $dst && cp -r ./build/docs/$docFormat $dst"
}

/**
 * Generates a shell command for copying shuffleboard documentation. This is a shortcut for
 * <tt>docCopyCommand(artifact, "$artifact/javadoc/*")</tt>
 *
 * @param artifact the shuffleboard artifact to copy the docs for
 */
def shuffleboardDocCopyCommand(artifact) {
    return docCopyCommand(artifact, "$artifact/javadoc/*")
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
                shell(docCopyCommand("cpp", "doxygen/html/*"))
                shell(docCopyCommand("java", "javadoc/*"))
                shell(shuffleboardDocCopyCommand("shuffleboard/api"))
                shell(shuffleboardDocCopyCommand("shuffleboard/plugin/base"))
                shell(shuffleboardDocCopyCommand("shuffleboard/plugin/networktables"))
                shell(shuffleboardDocCopyCommand("shuffleboard/plugin/cameraserver"))
            }
        }
        publishers {
            archiveArtifacts {
                pattern('build/*Docs.zip')
            }
        }
    }
}
