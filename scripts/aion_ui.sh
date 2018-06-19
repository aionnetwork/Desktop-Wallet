#!/bin/bash

SCRIPT_PATH=$(dirname $(realpath $0))

cd ${SCRIPT_PATH}

STORAGE_DIR=${HOME}/.aion
java -cp "${SCRIPT_PATH}/mod/*:${SCRIPT_PATH}/lib/*" -Dlocal.storage.dir=${STORAGE_DIR} -Xms300m -Xmx500m org.aion.wallet.WalletApplication >> ${STORAGE_DIR}/log_file 2>&1 &
