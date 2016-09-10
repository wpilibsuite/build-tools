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
            postBuildTask {
                task('BUILD_SUCCESSFUL', "rm -rf $location && mkdir -p $location && cp -r ./edu.wpi.first.wpilib" +
                        ".updatesite/target/site/* $location")
            }
        }
    }
}