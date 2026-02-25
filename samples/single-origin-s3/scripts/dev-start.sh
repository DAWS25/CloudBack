#!/usr/bin/env bash
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
pushd "$DIR/.."
echo "script [$0] started date[$(date)] pwd[$(pwd)]"
##

docker compose down || true
docker compose up

##
popd
echo "script [$0] completed date[$(date)]"