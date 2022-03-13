#!/usr/bin/env bash

MODE=""
URL="https://localhost:8888"
ENABLE_TLS="false"

print_usage() {
  printf "Usage: ./http1.sh -c -t -u https://localhost:8888 or ./http1.sh -s -t -u 8888"
}

while getopts 'cstu:p:' flag; do
  case "${flag}" in
    c) MODE="Client" ;;
    s) MODE="Server" ;;
    t) ENABLE_TLS="true" ;;
    u) URL="${OPTARG}" ;;
    *) print_usage
       exit 1 ;;
  esac
done

java \
    -cp target/hoq-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    ca.uvic.hoq.Http1${MODE} $URL $ENABLE_TLS

