#!/bin/bash
set -eo pipefail

dir=$(dirname "$0")

pushd $dir/../
./mvnw install -DskipTests
popd
