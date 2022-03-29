#!/usr/bin/env python2

from sys import argv

from mininet.net import Mininet
from mininet.node import Controller
from mininet.topo import Topo
from mininet.link import TCLink
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel, info

class TestTopo(Topo):
  def build(self, n=2): # Default: 2 hosts connected to 1 switch: h1--s1--h2
      switch = self.addSwitch('s1')
      for h in range(n):
        # Add host
        host = self.addHost('h%s' % (h + 1))

        # 10 Mbps, 5ms delay, no packet loss
        self.addLink(host, switch, bw=10, delay='5ms', loss=0)

def main():
  topo = TestTopo() 
  net = Mininet(topo=topo, link=TCLink, controller=Controller)

  net.start()

  info("Dumping host connections\n")
  dumpNodeConnections(net.hosts)

  h1, h2  = net.hosts[0], net.hosts[1]

  print(h2.cmd('./run.sh -s -u https://{}:8888 -v 3 &'.format(h2.IP())))

  print(h1.cmd('./run.sh -c -u https://{}:8888 -v 3'.format(h2.IP())))

  net.stop()

if __name__ == '__main__':
  setLogLevel('info')
  main()
