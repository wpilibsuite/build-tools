package driverstation

import (
	"fmt"
	"log"
	"net"
	"sync"
	"time"
)

// Constants for the driverstation
const (
	version_number = "10020800"
	send_port      = 1110
	recv_port      = 1150
)

// DS is a DriverStation to talk to FRC robots.
type DS struct {
	// Communications
	send net.Conn
	recv *net.UDPConn

	// Control
	m sync.Mutex

	// Data
	team     int32
	loop     int32
	enabled  bool
	state    State
	alliance Alliance
	station  Station
	sync     uint32
}

// New creates a new DriverStation that connects to the robot immediately.
func New(team int32) *DS {
	return &DS{
		team:     team,
		state:    Teleop,
		alliance: Red,
		station:  Station1,
		sync:     2,
	}
}

// Connect sets up the send and receive UDP connections.
func (ds *DS) Connect() error {
	var err error
	ds.send, err = net.Dial("udp", fmt.Sprintf("127.0.0.1:%d", send_port))
	if err != nil {
		return err
	}

	addr, err := net.ResolveUDPAddr("udp", fmt.Sprintf(":%d", recv_port))
	if err != nil {
		return err
	}
	ds.recv, err = net.ListenUDP("udp", addr)
	if err != nil {
		return err
	}

	return nil
}

// Run sends and receives data with the robot to allow control.
func (ds *DS) Run() {
	send_time := time.Tick(20 * time.Millisecond)
	reads := ds.receive()
	for {
		select {
		case <-send_time:
			ds.m.Lock()
			ds.loop += 1
			ds.m.Unlock()

			_, err := ds.send.Write(ds.packData())
			if err != nil {
				log.Fatal(err)
			}

		case r := <-reads:
			if r.err != nil {
				log.Fatal(r.err)
			}
			if r.n < 6  {
				log.Fatal("Didn't receive full packet.")
			}
			// TODO: log.Println("Received message.")
		}
	}
}

// SetEnabled enables and disables the robot.
func (ds *DS) SetEnabled(enabled bool) *DS {
	ds.m.Lock()
	defer ds.m.Unlock()
	ds.enabled = enabled
	return ds
}

// SetState switches the robot between Teleop, Auto and Test modes.
func (ds *DS) SetState(state State) *DS {
	ds.m.Lock()
	defer ds.m.Unlock()
	ds.state = state
	return ds
}

// SetAlliance sets the alliance to red or blue.
func (ds *DS) SetAlliance(alliance Alliance) *DS {
	ds.m.Lock()
	defer ds.m.Unlock()
	ds.alliance = alliance
	return ds
}

// SetStation sets the station too 1, 2 or 3.
func (ds *DS) SetStation(station Station) *DS {
	ds.m.Lock()
	defer ds.m.Unlock()
	ds.station = station
	return ds
}

// Resync synchronizes the driverstation with the robot.
func (ds *DS) Resync() *DS {
	ds.m.Lock()
	defer ds.m.Unlock()
	ds.sync = 2
	return ds
}

type read struct {
	buff []byte
	n    int
	err  error
}

func (ds *DS) receive() <-chan *read {
	ch := make(chan *read)
	go func() {
		for {
			buff := make([]byte, 1024)
			n, err := ds.recv.Read(buff)
			ch <- &read{buff, n, err}
		}
	}()
	return ch
}

func (ds *DS) packData() []byte {
	buff := make([]byte, 6)
	ds.m.Lock()
	defer ds.m.Unlock()

	// Add loops (2 bytes)
	buff[0] = byte(ds.loop >> 8)
	buff[1] = byte(ds.loop)
	
	buff[2] = 0x01

	// Add Status (4 bytes)
	buff[3] = ds.status()
	buff[4] = 0x10 // start program

	// TODO: Alliance R/B, 1/2/3 (2 bytes)
	if ds.alliance == Red {
		buff[5] = byte(ds.station) - 1
	} else {
		buff[5] = 2 + byte(ds.station)
	}

	return buff
}

// Bit flags for the status byte
//
// Bits
// 0: FPGA Checksum
// 1: Test Mode
// 2: Resynch
// 3: FMS Attached
// 4: Auto
// 5: Enabled
// 6: Not E-Stopped
// 7: Reset
const (
	flagTest byte = 1 << iota
	flagAuto
	flagEnabled
	flagFMSAttached
	flagReserved4
	flagReserved5
	flagReserved6
	flagEStopped
)

// status returns the status byte.
//
// Note: FPGA Checksum, FMS Attached are never used.
func (ds *DS) status() byte {
	var b byte = 0

	if ds.state == Auto {
		b |= flagAuto
	} else if ds.state == Test {
		b |= flagTest
	}

	if ds.enabled {
		b |= flagEnabled
	}


	return b
}
