#!/bin/bash

SCRIPT_PATH=$(dirname $(realpath $0))

cd ${SCRIPT_PATH}

MOD_DIR=${SCRIPT_PATH}/mod/*
LIB_DIR=${SCRIPT_PATH}/lib/*
STORAGE_DIR=${HOME}/.aion

java -cp "${MOD_DIR}:${LIB_DIR}" -Dlocal.storage.dir=${STORAGE_DIR} -Xms300m -Xmx500m org.aion.wallet.WalletApplication
