export CATALINA_OPTS="\
    -server \
    -Dsdlc=dev \
    -XX:+DisableExplicitGC \
    -XX:+PrintGCDateStamps \
    -verbose:gc \
    -XX:+PrintGCDetails \
    -Xloggc:${CATALINA_HOME}/logs/gc.log \
    -XX:+UseGCLogFileRotation \
    -XX:NumberOfGCLogFiles=5 \
    -XX:GCLogFileSize=50M \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=America/New_York \
    -Dsun.net.inetaddr.ttl=30 \
    ${CATALINA_OPTS}"
