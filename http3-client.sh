#!/usr/bin/env bash

URL=${1:-"https://quic.tech:8443"}

java \
    -cp target/hoq-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    ca.uvic.hoq.Http3Client $URL

