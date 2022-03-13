#!/usr/bin/env bash

print_usage() {
  printf "Usage:\nHTTP/1.1 Server: ./run.sh -s -u https://localhost:8888 -v 1 (-t)\nHTTP/1.1 Client: ./run.sh -c -u 8888 -v 1 (-t)\nQUIC Server: ./run.sh -s -u 8888 -v 3\nQUIC Client: ./run.sh -c -u https://localhost:8888 -v 3\n"
}

if [ $OPTIND -eq 1 ]; then
  print_usage;
  exit 1;
fi

ENABLE_TLS="false"

while getopts 'cstv:u:' flag; do
  case "${flag}" in
    c) MODE="Client" ;;
    s) MODE="Server" ;;
    t) ENABLE_TLS="true" ;;
    v) VERSION="${OPTARG}" ;;
    u) URL="${OPTARG}" ;;
    *) print_usage
       exit 1 ;;
  esac
done

java \
    -cp target/hoq-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    ca.uvic.hoq.Http${VERSION}${MODE} $URL $ENABLE_TLS

