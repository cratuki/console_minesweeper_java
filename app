#!/bin/bash

set -e

java_files=`find pkg -name "*.java"`

echo $java_files

javac -d build $java_files

java -cp $CLASSPATH:build pkg.Main


