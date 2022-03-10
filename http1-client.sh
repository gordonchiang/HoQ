#!/usr/bin/env bash

ENABLE_TLS="$1"

java \
    -cp target/hoq-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    ca.uvic.hoq.Http1Client $ENABLE_TLS

