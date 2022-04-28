# HoQ

HL7-over-QUIC (HoQ) is a program to test the performance of the Java HL7v2 API [HAPI](https://github.com/hapifhir/hapi-hl7v2) running over a Java implementation of QUIC [quiche4j](https://github.com/kachayev/quiche4j).

HoQ was developed with help from examples from [quiche4j](https://github.com/kachayev/quiche4j/tree/master/quiche4j-examples/src/main/java/io/quiche4j/examples), [HAPI](https://hapifhir.github.io/hapi-hl7v2/hapi-hl7overhttp/doc_hapi.html), and [Saravanan Subramanian](https://saravanansubramanian.com/hl7tutorials/#hl7-programming-tutorials-using-hapi-and-java).

A dataset and graphs can be found in the `data/` directory.

## Getting Started

HoQ was developed for Ubuntu 20.04 LTS using OpenJDK 11. Other distributions and Java versions have not been tested.

1. Clone quiche4j and build
2. Clone HoQ and build: `mvn install`
3. Run `./run.sh` to see how to run the clients and servers

## Mininet Testing

1. Install Mininet: `sudo apt-get install mininet`
2. Install other Mininet utils:

	```
    git clone git@github.com:mininet/mininet.git
    mininet/util/install.sh -fw
    ```
3. Run `sudo ./test.sh` to see how to run the test script

### Automated Testing

1. Examine the dump_*.sh scripts in the `dump/` directory
2. Adjust the script parameters to suit your experimental parameters
3. Run script `./dumps/dump_*.sh` to run experiments (Note: script will request `sudo` to run Mininet)
4. Run script `./dumps/calculate.sh` to automatically calculate the results i.e. HL7v2 transaction duration times
5. Run the corresponding `./dumps/graph_*.py` script to generate SVG graphs of your results

## Troubleshooting

If you run into build errors for quiche4j, try to switch the quiche4j directory to use the nightly toolchain of cargo: `rustup override set nightly`

If you see `WARNING: An illegal reflective access operation has occurred`, [try the solution here](https://stackoverflow.com/a/63876216).
