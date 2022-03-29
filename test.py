#!/usr/bin/env python2

from mininet.net import Mininet
from mininet.topo import MinimalTopo
from mininet.node import Controller
from mininet.log import setLogLevel

def main():
  topo = MinimalTopo() # 2 hosts connected to 1 switch: h1--s1--h2
  net = Mininet(topo=topo, controller=Controller)

  net.start()

  h1, h2  = net.hosts[0], net.hosts[1]

  print(h2.cmd('./run.sh -s -u https://{}:8888 -v 3 &'.format(h2.IP())))

  print(h1.cmd('./run.sh -c -u https://{}:8888 -v 3'.format(h2.IP())))


  net.stop()

if __name__ == '__main__':
  setLogLevel('info')
  main()
