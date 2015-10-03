
export GOPATH=${CURDIR}
PREFIX ?= /usr/local

all:
	go install go.thoriumrobotics.com/frc.v0/cmds/...

clean:
	find src -type f -executable | xargs rm -f
	rm -rf pkg/ bin/

install:
	mkdir -p ${DESTDIR}${PREFIX}/bin/
	install -o root -g root bin/linux_arm/*  ${DESTDIR}${PREFIX}/bin/
	install -o root -g root teststand-launch  ${DESTDIR}${PREFIX}/bin/

