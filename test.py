#!/usr/bin/env python2

from mininet.net import Mininet
from mininet.topo import MinimalTopo
from mininet.node import Controller

topo = MinimalTopo() # 2 hosts connected to 1 switch: h1--s1--h2
net = Mininet(topo=topo, controller=Controller)

net.start()

h1, h2  = net.hosts[0], net.hosts[1]
print(h1.cmd('ping -c1 %s' % h2.IP()))

net.stop()
