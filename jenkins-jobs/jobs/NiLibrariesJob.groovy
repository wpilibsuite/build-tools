def basePath = 'ni-libraries'
folder(basePath)

def releaseJob = pipelineJob("$basePath/ni-libraries - Release") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/ni-libraries-release.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/ni-libraries-release.groovy'))
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
            githubProjectUrl('https://github.com/wpilibsuite/ni-libraries')
        }
        logRotator {
            numToKeep(50)
            artifactNumToKeep(10)
        }
    }
}
