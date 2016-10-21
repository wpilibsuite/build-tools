def basePath = 'Releases'
folder(basePath)

['Beta', 'Stable', 'Release'].each { buildType ->
    def job = pipelineJob("$basePath/$buildType") {
        definition {
            cps {
                try {
                    script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/uberbuild.groovy').replaceAll('BUILD_TYPE', buildType))
                } catch (Exception e) {
                    script(readFileFromWorkspace('pipeline-scripts/uberbuild.groovy').replaceAll('BUILD_TYPE', buildType))
                }
                sandbox()
            }
        }
    }
}