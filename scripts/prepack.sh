# generate aion runtime

echo "Current path: "$PWD
JDK_RT="rt"

#Check JAVA_HOME
echo "Your JAVA_HOME path: "$JAVA_HOME


# 8 for 1.8.0_nn, 9 for 9-ea etc, and "no_java" for undetected
jdk_version() {
  local result
  local java_cmd

  # reasoning behind checking $JAVA_HOME first is ant defers to it for builds
  # fixes some issues with our CI
  if [[ (-n "$JAVA_HOME") && (-x "$JAVA_HOME/bin/java") ]]
  then
    java_cmd="$JAVA_HOME/bin/java"
  elif [[ -n $(type -p java) ]]
  then
    java_cmd=java
  fi

  local IFS=$'\n'
  # remove \r for Cygwin
  local lines=$("$java_cmd" -Xms32M -Xmx32M -version 2>&1 | tr '\r' '\n')
  if [[ -z $java_cmd ]]
  then
    result=no_java
  else
    for line in $lines; do
      if [[ (-z $result) && ($line = *"version \""*) ]]
      then
        local ver=$(echo $line | sed -e 's/.*version "\(.*\)"\(.*\)/\1/; 1q')
        # on macOS, sed doesn't support '?'
        if [[ $ver = "1."* ]]
        then
          result=$(echo $ver | sed -e 's/1\.\([0-9]*\)\(.*\)/\1/; 1q')
        else
          result=$(echo $ver | sed -e 's/\([0-9]*\)\(.*\)/\1/; 1q')
        fi
      fi
    done
  fi
  echo "$result"
}


JAVA_VER="$(jdk_version)"
echo "Java ver: "$JAVA_VER

if [ $JAVA_VER != 10 ]; then
    echo "Please use JDK10 to build the project."
    exit 1
fi

if [ ! -d "$JDK_RT" ]; then
  mkdir rt
  $JAVA_HOME/bin/jlink --add-modules java.base,java.xml,java.logging,java.management,jdk.unsupported,java.desktop,java.naming,java.sql,jdk.sctp,javafx.base,javafx.graphics,javafx.controls,javafx.fxml,javafx.swing \
                       --output $JDK_RT/linux --compress 2 --strip-debug
  cp $JAVA_HOME/bin/jstack $JDK_RT/linux/bin
fi
