#!/usr/bin/env bash

MODE=""
URL="https://localhost:8888"

print_usage() {
  printf "Usage: ./http3.sh -c -u https://localhost:8888 or ./http3.sh -s -u 8888"
}

while getopts 'cstu:p:' flag; do
  case "${flag}" in
    c) MODE="Client" ;;
    s) MODE="Server" ;;
    u) URL="${OPTARG}" ;;
    *) print_usage
       exit 1 ;;
  esac
done

java \
    -cp target/hoq-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    ca.uvic.hoq.Http3${MODE} $URL

