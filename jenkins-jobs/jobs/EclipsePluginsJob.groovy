job('Eclipse Plugins') {
    scm {
        git('https://github.com/wpilibsuite/EclipsePlugins.git')
    }
    triggers {
        scm('H/15 * * * *')
    }
    steps {
        gradle('clean')
        gradle('build')
    }
}
