# HoQ

HL7-over-QUIC (HoQ) is a program to test the performance of the Java HL7v2 API [HAPI](https://github.com/hapifhir/hapi-hl7v2) running over a Java implementation of QUIC [quiche4j](https://github.com/kachayev/quiche4j).

HoQ was developed with help from examples from [quiche4j](https://github.com/kachayev/quiche4j/tree/master/quiche4j-examples/src/main/java/io/quiche4j/examples), [HAPI](https://hapifhir.github.io/hapi-hl7v2/hapi-hl7overhttp/doc_hapi.html), and [Saravanan Subramanian](https://saravanansubramanian.com/hl7tutorials/#hl7-programming-tutorials-using-hapi-and-java).

## Getting Started

HoQ was developed for Ubuntu 20.04 LTS using OpenJDK 11. Other distributions and Java versions have not been tested.

1. Clone [a fork of quiche4j with minor fixes](https://github.com/gordonchiang/quiche4j/tree/feature-Enable_sending_body_with_request) and build
2. Clone HoQ and build: `mvn install`
3. Use `./run.sh` to see how to run the clients and servers

## Mininet Testing

sudo apt-get install mininet

git clone git@github.com:mininet/mininet.git

mininet/util/install.sh -fw

sudo mn -c

sudo mn --topo single,4  --link tc,bw=10,delay=100ms,loss=1

h1 ./run.sh -c -u https://10.0.0.2:8888 -v 3

h2 ./run.sh -s -u https://10.0.0.2:8888 -v 3 &

h3 ./run.sh -c -u https://10.0.0.4:8888 -v 1

h4 ./run.sh -s -u https://10.0.0.4:8888 -v 1 &

h3 ./run.sh -c -u https://10.0.0.4:8888 -v 1 -t

h4 ./run.sh -s -u https://10.0.0.4:8888 -v 1 -t &

## Troubleshooting

If you run into build errors for quiche4j, try to switch the quiche4j directory to use the nightly toolchain of cargo: `rustup override set nightly`

If you see `WARNING: An illegal reflective access operation has occurred`, [try the solution here](https://stackoverflow.com/a/63876216).
