#!/bin/sh

java $JVM_OPTS -cp target/incidents-standalone.jar clojure.main -m incidents.scrape
