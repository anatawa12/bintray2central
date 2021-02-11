#!/usr/bin/env bash

./gradlew jpackage

pushd build/jpackage || exit
tar -czf bintray2central.tar.gz bintray2central
rm -rf bintray2central
popd || exit

mkdir -p built
cp ./build/jpackage/* built/
