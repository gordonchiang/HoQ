#!/usr/bin/env python2

import getopt
from sys import argv
from time import sleep, localtime

from mininet.net import Mininet
from mininet.node import Controller
from mininet.topo import Topo
from mininet.link import TCLink
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel, info

http_version = None
tls = ''

class TestTopo(Topo):
  def build(self, n=2): # Default: 2 hosts connected to 1 switch: h1--s1--h2
    switch = self.addSwitch('s1')
    for h in range(n):
      # Add hosts
      host = self.addHost('h%s' % (h + 1))

      # 10 Mbps, 5ms delay, no packet loss
      self.addLink(host, switch, bw=10, delay='5ms', loss=0)

def main():
  topo = TestTopo() 
  net = Mininet(topo=topo, link=TCLink, controller=Controller)

  net.start()

  info('Dumping host connections\n')
  dumpNodeConnections(net.hosts)

  h1, h2, s1  = net.hosts[0], net.hosts[1], net.switches[0]

  now = localtime()
  filename = '{}{}{}{}{}{}_s1_dump.pcap'.format(now[0], now[1], now[2], now[3], now[4], now[5])
  info('Recording packets: {}\n'.format(filename))
  s1_pcap = s1.popen(['wireshark', '-i', 's1-eth1', '-i', 's1-eth2', '-f', 'port 8888', '-k', '-w', './dumps/{}'.format(filename)]) # TODO: -f doesn't seem to work

  sleep(10)

  info('Starting server\n')
  h2.cmd('./run.sh -s -u https://{}:8888 -v {} {} &'.format(h2.IP(), http_version, tls))

  info('Starting client\n')
  print(h1.cmd('./run.sh -c -u https://{}:8888 -v {} {}'.format(h2.IP(), http_version, tls)))

  sleep(10)

  s1_pcap.terminate()

  net.stop()

if __name__ == '__main__':
  setLogLevel('info')

  try:
    # Parse options
    arguments, values = getopt.getopt(argv[1:], "tv:")
    
    # Check arguments
    for currentArgument, currentValue in arguments:
      if currentArgument in ('-v'):
        http_version = currentValue

      elif currentArgument in ('-t'):
        tls = '-t'

    if not http_version:
      exit()

  except getopt.error as err:
    # Error parsing command line arguments
    print (str(err))

  main()
