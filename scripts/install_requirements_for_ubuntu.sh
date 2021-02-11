#!/usr/bin/env bash

JAVA_VERSION="15.0.2"
JAVA_BUILD="7"
JAVA_VERSION2="15.0.2+7"

check_install() {
  if ! hash "$1" 2>/dev/null; then
    apt update
    apt install -y "$1"
  fi
}

# install java (15)
if ! hash java 2>/dev/null; then
  check_install wget
  wget "https://github.com/AdoptOpenJDK/openjdk15-binaries/releases/download/jdk-$JAVA_VERSION%2B$JAVA_BUILD/OpenJDK15U-jdk_x64_linux_hotspot_${JAVA_VERSION}_${JAVA_BUILD}.tar.gz"
  tar xzf "OpenJDK15U-jdk_x64_linux_hotspot_${JAVA_VERSION}_${JAVA_BUILD}.tar.gz"
  export PATH="$PWD/$JAVA_VERSION2/bin:$PATH"
  java -version 2>/dev/null || ( echo "not found" )
fi

if ! hash rpm 2>/dev/null; then
  check_install aptitude
  apt update
  aptitude install -y rpm
fi

# install fakeroot
check_install fakeroot
