#!/usr/bin/env bash
CURR_SCRIPT="${0}"
java -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:MaxGCPauseMillis=2000 -Xms4000m -Xmx30000m  -jar "${CURR_SCRIPT%/*}"/igv-nrgene-*.jar $*
