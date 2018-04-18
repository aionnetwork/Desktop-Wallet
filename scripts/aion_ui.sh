#!/bin/bash

SCRIPT_PATH=$(dirname $(realpath $0))

pushd ${SCRIPT_PATH}

java -cp "${SCRIPT_PATH}/mod/*:${SCRIPT_PATH}/lib/*" org.aion.wallet.WalletApplication

popd
