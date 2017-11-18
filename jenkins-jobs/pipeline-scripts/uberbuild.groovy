node {
    build job: 'RobotBuilder/RobotBuilder - Release', propagate: false
    build job: 'Java Installer/Java Installer - Release', propagate: false
    build job: 'wpiutil/wpiutil - Release', propagate: false
    build job: 'ntcore/ntcore - Release', propagate: false
    build job: 'CSCore/cscore - Release', propagate: false
    build job: 'SmartDashboard/SmartDashboard - Release', propagate: false
    build job: 'OutlineViewer/OutlineViewer - Release', propagate: false
    build job: 'Shuffleboard/Shuffleboard - Release', propagate: false
    build job: 'WPILib/WPILib - Release', propagate: false
    build job: 'Documentation/Documentation - BUILD_TYPE', propagate: false
    build job: 'Eclipse Plugins/Eclipse Plugins - BUILD_TYPE'
}
