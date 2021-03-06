#!/usr/bin/env bash

params="-b 100 -i ${1:-10}" # 100 Mbps, 10 iterations

git checkout main # use main branch aka 1 message
mvn install

for loss in 1 5 10;
do

  for delay in 1ms 5ms 10ms;
  do
    extra_params="-l ${loss} -d ${delay}"
    dir_addons="${loss}%_${delay}"

    # Main branch, wifi, no TLS
    sudo ./test.py -v 1 ${params} ${extra_params}

    dir_name="main_wifi_no_tls_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}

    # Main branch, wifi, TLS
    sudo ./test.py -v 1 -t ${params} ${extra_params}

    dir_name="main_wifi_tls_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}

    # Main branch, wifi, QUIC
    sudo ./test.py -v 3 ${params} ${extra_params}

    dir_name="main_wifi_quic_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}

  done

done

git checkout gordonchiang/Send_multiple_requests # use branch for 10 messages at once
mvn install

for loss in 1 5 10;
do

  for delay in 1ms 5ms 10ms;
  do
    extra_params="-l ${loss} -d ${delay}"
    dir_addons="${loss}%_${delay}"

    # Mult branch, wifi, no TLS
    sudo ./test.py -v 1 ${params} ${extra_params}

    dir_name="mult_wifi_no_tls_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}

    # Mult branch, wifi, TLS
    sudo ./test.py -v 1 -t ${params} ${extra_params}

    dir_name="mult_wifi_tls_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}

    # Mult branch, wifi, QUIC
    sudo ./test.py -v 3 ${params} ${extra_params}

    dir_name="mult_wifi_quic_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}

  done

done
