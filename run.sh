#!/bin/bash
mkdir -p out
javac -d out src/*.java || exit 1
echo "Open http://localhost:8080/ in your browser."
java -cp out SatServer