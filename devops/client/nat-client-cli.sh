#!/bin/sh

export CUR_DIR="$(  pwd  )"
#echo "Using CUR_DIR: ${CUR_DIR} "
export SHELL_DIR="$( cd "$( dirname "$0"  )" && pwd  )"
#echo "Using SHELL_DIR: ${SHELL_DIR} "
export BASE_DIR="$( cd "${SHELL_DIR}/." && pwd  )"
#echo "Using BASE_DIR: ${BASE_DIR}"

APP_NAME="entdiy-nat-client"
MAX_TIMEOUT=10

case "$1" in
    startup)
    echo Startup ${APP_NAME} ...
    nohup java -jar -Dspring.profiles.active=prd ${BASE_DIR}/${APP_NAME}.jar &
    echo Application logs write to file: ${BASE_DIR}/logs/${APP_NAME}.log
    tail -f ${BASE_DIR}/nohup.out
    ;;
    shutdown)
    echo Shutdown ${APP_NAME} ...

    tpid=`ps -ef|grep ${APP_NAME} | grep java |grep -v grep |grep -v kill|awk '{print $2}'`
    if [ -n "${tpid}" ]; then
      (kill -15 $tpid)
    fi
    for((i=1;i<$MAX_TIMEOUT;i++))
    do
           sleep 1
           cnt=i+1
           tpid=`ps -ef|grep ${APP_NAME} | grep java |grep -v grep|grep -v kill|awk '{print $2}'`
           if [ -n "${tpid}" ]; then
                   echo "Waiting to stop ${APP_NAME} ${cnt}/${MAX_TIMEOUT}s"
           else
                   break
           fi
    done

    tpid=`ps -ef|grep ${APP_NAME} | grep java |grep -v grep|grep -v kill|awk '{print $2}'`
    if [ -n "${tpid}" ]; then
           echo "Force kill ${APP_NAME} ..."
           (kill -9 $tpid)
    fi
    echo "Stopped ${APP_NAME}"
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
