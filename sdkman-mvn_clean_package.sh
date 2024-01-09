#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")"

############################################################

./sdkman-mvn.sh clean package
