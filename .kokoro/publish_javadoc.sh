#!/bin/bash
set -eo pipefail

pwd

dir=$(dirname "$0")

pushd $dir/../
./mvnw clean javadoc:aggregate
pwd
popd

pwd
# install docuploader package
python3 -m pip install gcp-docuploader
