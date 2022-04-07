#!/usr/bin/env python2

import getopt
from sys import argv
from time import sleep, localtime

from mininet.net import Mininet
from mininet.topo import Topo
from mininet.link import TCLink
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel, info

http_version = 1 # 1 or 3
tls = '' # '-t' or empty string
bandwidth = 10 # Mbps
delay = '0ms' # ms
loss = 0 # %
iterations = 1

def printUsage():
  print('''Example: sudo ./test.sh -v 1 -t -b 10 -d 100ms -l 1 -i 10
-v to choose HTTP version (1 or 3) (defualt is 1)
-t to enable TLS on HTTP/1.1 (default is disabled)
-b to select bandwidth of links (default is 10 Mbps)
-d to select delay of links (default is 0ms)
-l to select loss at links (default is 0%)
-i to select the number of test iterations to run (default is 1)''')

class TestTopo(Topo):
  def build(self, n=2): # 2 hosts connected to 1 switch: h1--s1--h2
    switch = self.addSwitch('s1')
    for h in range(n):
      # Add hosts
      host = self.addHost('h%s' % (h + 1))

      # Create link from client to switch as bottleneck
      # with selected bandwidth, delay, and loss (default is 10 Mbps, 0ms delay, 0% loss)
      if h == 0:
        self.addLink(host, switch, bw=bandwidth, delay=delay, loss=loss)

      # From server to switch, assume direct ethernet connection (no bottleneck)
      else:
        self.addLink(host, switch, bw=1000, delay='0ms', loss=0)

def main():
  topo = TestTopo() 
  
  for i in range(iterations):
    info('\n----- Starting iteration {}/{} -----\n'.format(i+1, iterations))

    net = Mininet(topo=topo, link=TCLink, controller=None)
    net.start()

    info('Dumping host connections\n')
    dumpNodeConnections(net.hosts)

    h1, h2, s1  = net.hosts[0], net.hosts[1], net.switches[0]

    info('Adding flows to switch')
    print(s1.cmd('ovs-ofctl add-flow s1 in_port=1,nw_dst=10.0.0.2,actions=output:2'))
    print(s1.cmd('ovs-ofctl add-flow s1 in_port=2,nw_dst=10.0.0.1,actions=output:1'))

    now = localtime()
    filename = '{}{}{}{}{}{}_s1_dump.pcap'.format(now[0], now[1], now[2], now[3], now[4], now[5])
    info('Recording packets: {}\n'.format(filename))
    s1_pcap = s1.popen(['wireshark', '-i', 's1-eth1', '-i', 's1-eth2', '-f', 'port 8888', '-k', '-w', './dumps/{}'.format(filename)]) # TODO: -f doesn't seem to work

    sleep(3)

    info('Starting server\n')
    h2.cmd('./run.sh -s -u https://{}:8888 -v {} {} &'.format(h2.IP(), http_version, tls))

    sleep(2)

    info('Starting client\n')
    print(h1.cmd('./run.sh -c -u https://{}:8888 -v {} {}'.format(h2.IP(), http_version, tls)))

    sleep(3)

    s1_pcap.terminate()

    net.stop()

    info('----- End of iteration {}/{} -----\n\n'.format(i+1, iterations))

if __name__ == '__main__':
  setLogLevel('info')

  if len(argv) <= 1:
    printUsage()
    exit(1)

  try:
    # Parse options
    arguments, values = getopt.getopt(argv[1:], "tv:b:d:l:i:")
    
    # Check arguments
    for currentArgument, currentValue in arguments:
      if currentArgument in ('-v'):
        http_version = currentValue

      elif currentArgument in ('-t'):
        tls = '-t'

      elif currentArgument in ('-b'):
        bandwidth = int(currentValue)
      
      elif currentArgument in ('-d'):
        delay = currentValue

      elif currentArgument in ('-l'):
        loss = int(currentValue)

      elif currentArgument in ('-i'):
        iterations = int(currentValue)

  except getopt.error as err:
    # Error parsing command line arguments
    print(str(err))
    printUsage()
    exit(1)

  main()
