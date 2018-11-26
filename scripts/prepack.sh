#@IgnoreInspection BashAddShebang
# generate aion runtime

echo "Current path: "$PWD
JDK_RT="rt"

#Check JAVA_HOME
echo "Your JAVA_HOME path: "${JAVA_HOME}


# 8 for 1.8.0_nn, 9 for 9-ea etc, and "no_java" for undetected
get_jdk_version() {
  local RESULT
  local JAVA_CMD

  # reasoning behind checking $JAVA_HOME first is ant defers to it for builds
  # fixes some issues with our CI
  if [[ (-n "$JAVA_HOME") && (-x "$JAVA_HOME/bin/java") ]]
  then
    JAVA_CMD="$JAVA_HOME/bin/java"
  elif [[ -n $(type -p java) ]]
  then
    echo "Please set JAVA_HOME to a correct destination"
    exit 1
  fi

  local IFS=$'\n'
  # remove \r for Cygwin
  local lines=$("$JAVA_CMD" -Xms32M -Xmx32M -version 2>&1 | tr '\r' '\n')
  if [[ -z $JAVA_CMD ]]
  then
    RESULT="no_java"
  else
    for line in $lines; do
      if [[ (-z ${RESULT}) && ($line = *"version \""*) ]]
      then
        local VERSION=$(echo $line | sed -e 's/.*version "\(.*\)"\(.*\)/\1/; 1q')
        # on macOS, sed doesn't support '?'
        if [[ ${VERSION} = "1."* ]]
        then
          RESULT=$(echo ${VERSION} | sed -e 's/1\.\([0-9]*\)\(.*\)/\1/; 1q')
        else
          RESULT=$(echo ${VERSION} | sed -e 's/\([0-9]*\)\(.*\)/\1/; 1q')
        fi
      fi
    done
  fi
  echo "${RESULT}"
}


JAVA_VER="$(get_jdk_version)"
echo "Java ver: "${JAVA_VER}

if [ ${JAVA_VER} != 11 ]; then
    echo "Please use JDK11 to build the project."
    exit 1
fi

if [ ! -d "${JDK_RT}" ]; then
  mkdir ${JDK_RT}
  OUTPUT=${JDK_RT}/java
  ${JAVA_HOME}/bin/jlink --add-modules java.base,java.xml,java.logging,java.management,jdk.unsupported,java.desktop,java.naming,java.sql,jdk.sctp,java.scripting \
  --output ${OUTPUT} --compress 2 --strip-debug
  cp ${JAVA_HOME}/bin/jstack ${OUTPUT}/bin
fi
