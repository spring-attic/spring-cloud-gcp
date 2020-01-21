#!/bin/bash
set -eo pipefail

# Get into the spring-cloud-gcp repo directory
dir=$(dirname "$0")
pushd $dir/../

# install docuploader package
python3 -m pip install --user gcp-docuploader



popd

