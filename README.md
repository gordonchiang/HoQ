# HoQ

Mininet testing

sudo mn --topo single,4  --link tc,bw=10,delay=100ms,loss=1

h1 ./run.sh -c -u https://10.0.0.2:8888 -v 3
h2 ./run.sh -s -u https://10.0.0.2:8888 -v 3 &

h3 ./run.sh -c -u https://10.0.0.4:8888 -v 1
h4 ./run.sh -s -u https://10.0.0.4:8888 -v 1 &

h3 ./run.sh -c -u https://10.0.0.4:8888 -v 1 -t
h4 ./run.sh -s -u https://10.0.0.4:8888 -v 1 -t &
