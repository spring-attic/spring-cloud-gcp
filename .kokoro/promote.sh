#!/bin/bash
# Copyright 2019-2020 Google LLC
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA

set -eo pipefail

# STAGING_REPOSITORY_ID must be set
if [ -z "${STAGING_REPOSITORY_ID}" ]; then
  echo "Missing STAGING_REPOSITORY_ID environment variable"
  exit 1
fi

dir=$(dirname "$0")

source $dir/common.sh

pushd $dir/../

MAVEN_SETTINGS_FILE=$(realpath .)/settings.xml

setup_environment_secrets
create_settings_xml_file $MAVEN_SETTINGS_FILE

./mvnw nexus-staging:release -B \
  --settings=settings.xml \
  -DstagingRepositoryId=${STAGING_REPOSITORY_ID} \
  -P release

popd
