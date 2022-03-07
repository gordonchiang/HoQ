#!/usr/bin/env bash

BIND=${1:-"localhost:4433"}

java \
    -cp target/hoq-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    ca.uvic.hoq.Http3Server $BIND

