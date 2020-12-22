#!/bin/sh

export CUR_DIR="$(  pwd  )"
#echo "Using CUR_DIR: ${CUR_DIR} "
export SHELL_DIR="$( cd "$( dirname "$0"  )" && pwd  )"
#echo "Using SHELL_DIR: ${SHELL_DIR} "
export BASE_DIR="$( cd "${SHELL_DIR}/." && pwd  )"
#echo "Using BASE_DIR: ${BASE_DIR}"

APP_NAME="entdiy-nat-client"
WAIT_SECONDS=10

case "$1" in
    startup)
    echo Startup ${APP_NAME} ...
    nohup java -jar -Dspring.profiles.active=prd ${BASE_DIR}/${APP_NAME}.jar > nohup.out 2>&1 &
    echo $! > ${BASE_DIR}/${APP_NAME}.pid
    echo Application logs write to file: ${BASE_DIR}/logs/${APP_NAME}.log
    tail -f ${BASE_DIR}/nohup.out
    ;;
    shutdown)
    echo Shutdown ${APP_NAME} ...
    if [ -e "${BASE_DIR}/${APP_NAME}.pid" ]; then
      PID=$(cat ${BASE_DIR}/${APP_NAME}.pid)
      PID_EXIST=$(ps aux | awk '{print $2}'| grep -w $PID)
      count=0
      while true
      do
        ((count++))
        if [ $count -eq 1 ];then
          kill -15 $PID
        fi
        sleep 1s
        echo Waiting ${count} seconds/${WAIT_SECONDS} ...
        if [ $count -gt $WAIT_SECONDS ];then
          kill -9 $PID
        fi
        PID_EXIST=$(ps aux | awk '{print $2}'| grep -w $PID)
        if [ ! $PID_EXIST ];then
          break;
        fi
      done
      rm -f ${BASE_DIR}/${APP_NAME}.pid
      echo Shutdown successfully.
    else
      echo "No pid file found, nothing to do."
    fi
    ps -ef|grep java| grep ${APP_NAME}
    ;;
    restart)
    $0 shutdown
    $0 startup
    ;;
    status)
    ps -ef|grep java| grep ${APP_NAME}
    ;;
    *)
    echo "Usage: $0 {startup|shutdown|restart|status}"
    exit 1
    ;;
esac
exit 0
