#!/usr/bin/env bash
CURR_SCRIPT="${0}"
java -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -Xmx3g -jar "${CURR_SCRIPT%/*}"/igv-nrgene-*.jar $*
