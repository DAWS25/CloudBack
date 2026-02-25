#!/usr/bin/env bash
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
pushd "$DIR/.."
echo "script [$0] started date[$(date)] pwd[$(pwd)]"
##

# sample content, replace with your own script
aws --version
cdk --version
pushd ..
find .
popd
echo "Sanity check completed"

##
popd
echo "script [$0] completed date[$(date)]"