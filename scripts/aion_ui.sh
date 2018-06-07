#!/bin/bash

SCRIPT_PATH=$(dirname $(realpath $0))

pushd ${SCRIPT_PATH}

java -cp "${SCRIPT_PATH}/mod/*:${SCRIPT_PATH}/lib/*" -Dlocal.storage.dir=${HOME}/.aion org.aion.wallet.WalletApplication

popd
