#!/usr/bin/env bash

params="-b 10 -i ${1:-3}" # 10 Mbps, 3 iterations

for loss in 1 3 5;
do

  for delay in 10ms 25ms 50ms;
  do
    extra_params="-l ${loss} -d ${delay}"
    dir_addons="${loss}%_${delay}"

    git checkout main # use main branch aka 1 message
    mvn install

    # Main branch, mobile, no TLS
    sudo ./test.py -v 1 ${params} ${extra_params}

    dir_name="main_mobile_no_tls_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}

    # Main branch, mobile, TLS
    sudo ./test.py -v 1 -t ${params} ${extra_params}

    dir_name="main_mobile_tls_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}

    # Main branch, mobile, QUIC
    sudo ./test.py -v 3 ${params} ${extra_params}

    dir_name="main_mobile_quic_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}



    git checkout gordonchiang/Send_multiple_requests # use branch for 10 messages in once
    mvn install

    # Mult branch, mobile, no TLS
    sudo ./test.py -v 1 ${params} ${extra_params}

    dir_name="mult_mobile_no_tls_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}

    # Mult branch, mobile, TLS
    sudo ./test.py -v 1 -t ${params} ${extra_params}

    dir_name="mult_mobile_tls_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}

    # Mult branch, mobile, QUIC
    sudo ./test.py -v 3 ${params} ${extra_params}

    dir_name="mult_mobile_quic_${dir_addons}"
    mkdir ./dumps/${dir_name}
    mv ./dumps/*.pcap ./dumps/${dir_name}

  done

done
