#!/usr/bin/env bash

./gradlew jpackage

mkdir -p built
cp -r ./build/jpackage/* built/
