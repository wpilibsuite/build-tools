node {
    build job: 'RobotBuilder/RobotBuilder - Release'
    build job: 'Java Installer/Java Installer - Release'
    build job: 'ntcore/ntcore - Release'
    build job: 'SmartDashboard/SmartDashboard - Release'
    build job: 'OutlineViewer/OutlineViewer - Release'
    build job: 'WPILib/WPILib - Release', propagation: false
    build job: 'Eclipse Plugins/Eclipse Plugins - BUILD_TYPE'
}