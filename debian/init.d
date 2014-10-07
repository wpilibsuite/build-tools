#!/bin/sh
### BEGIN INIT INFO
# Provides:          teststand
# Required-Start:    FRCNetComm 
# Required-Stop:     
# Default-Start:     5
# Default-Stop:      0 1 6
# Short-Description: TestStand
# Description:       Test Stand Software for FRC Jenkins
### END INIT INFO

DAEMON=/usr/local/frc/bin/teststand
DAEMONSTART=/usr/local/frc/bin/teststand-launch
NAME=teststand
DESC="TestStand Server"
ARGS="server"

test -f $DAEMON || exit 0

set -e

case "$1" in
    start)
        echo -n "* starting $DESC: $NAME... "
        start-stop-daemon -o -S -b -x $DAEMONSTART -- $DAEMON $ARGS
        echo "done."
        ;;
    stop)
        echo -n "* stopping $DESC: $NAME... "
        start-stop-daemon -K -x $DAEMON
        echo "done."
        ;;
    restart)
        echo "* restarting $DESC: $NAME... "
        $0 stop
        $0 start
        echo "done."
        ;;
    *)
        echo "Usage: $0 {start|stop|restart}"
        exit 1
        ;;
esac

exit 0

