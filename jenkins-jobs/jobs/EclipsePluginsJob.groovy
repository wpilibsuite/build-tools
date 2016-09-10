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
        def releaseFolder = "${System.getProperty('user.home')}/releases/release/eclipse/"
        shell("rm -rf $releaseFolder")
        shell("mkdir -p $releaseFolder")
        shell("cp -r ./edu.wpi.first.wpilib.plugins.updatesite/target/site $releaseFolder")
    }
}
