#!/bin/bash

SCRIPT_PATH=$(dirname $(realpath $0))

cd ${SCRIPT_PATH}

STORAGE_DIR=${HOME}/.aion
LOG_DIR=${STORAGE_DIR}/log
JAVA_INSTALL=${STORAGE_DIR}/jre-10.0.2
JAVA_CMD=${JAVA_INSTALL}/bin/java

if [ ! -f ${JAVA_CMD} ] || [ $(${JAVA_CMD} -version 2>&1 | grep "10.0.2" | wc -l) -lt 1 ]
 then
  mkdir -p ${JAVA_INSTALL}
  cp -r rt/linux/* ${JAVA_INSTALL}
  rm -r rt
fi

MOD_DIR=${SCRIPT_PATH}/mod/*
LIB_DIR=${SCRIPT_PATH}/lib/*

mkdir -p ${LOG_DIR}

${JAVA_CMD} -cp "${MOD_DIR}:${LIB_DIR}" -Dlocal.storage.dir=${STORAGE_DIR} -Xms300m -Xmx500m org.aion.wallet.WalletApplication &> ${LOG_DIR}/log &
