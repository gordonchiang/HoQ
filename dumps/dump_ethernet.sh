#!/usr/bin/env bash

git checkout main # use main branch aka 1 message
mvn install

params="-b 1000 -i ${1:-3}" # 1000 Mbps, 3 iterations

# Main branch, ethernet, no TLS
sudo ./test.py -v 1 ${params}

dir_name='main_ethernet_no_tls'
mkdir ./dumps/${dir_name}
mv ./dumps/*.pcap ./dumps/${dir_name}

# Main branch, ethernet, TLS
sudo ./test.py -v 1 -t ${params}

dir_name='main_ethernet_tls'
mkdir ./dumps/${dir_name}
mv ./dumps/*.pcap ./dumps/${dir_name}

# Main branch, ethernet, QUIC
sudo ./test.py -v 3 ${params}

dir_name='main_ethernet_quic'
mkdir ./dumps/${dir_name}
mv ./dumps/*.pcap ./dumps/${dir_name}

git checkout gordonchiang/Send_multiple_requests # use branch for 10 messages in once
mvn install

# Mult branch, ethernet, no TLS
sudo ./test.py -v 1 ${params}

dir_name='mult_ethernet_no_tls'
mkdir ./dumps/${dir_name}
mv ./dumps/*.pcap ./dumps/${dir_name}

# Mult branch, ethernet, TLS
sudo ./test.py -v 1 -t ${params}

dir_name='mult_ethernet_tls'
mkdir ./dumps/${dir_name}
mv ./dumps/*.pcap ./dumps/${dir_name}

# Mult branch, ethernet, QUIC
sudo ./test.py -v 3 ${params}

dir_name='mult_ethernet_quic'
mkdir ./dumps/${dir_name}
mv ./dumps/*.pcap ./dumps/${dir_name}
