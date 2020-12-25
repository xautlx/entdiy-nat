#!/bin/sh

export CUR_DIR="$(  pwd  )"
export SHELL_DIR="$( cd "$( dirname "$0"  )" && pwd  )"
export BASE_DIR="$( cd "${SHELL_DIR}/." && pwd  )"

#修改为服务端部署域名或主机公网IP
DOMAIN_HOST="127.0.0.1"
#证书密码
PASSWORD="entdiy-nat"
#证书有效期天数
VALIDITY_DAYS=3650

echo 生成服务端私钥和证书仓库...
keytool -genkey -alias entdiy-nat-server -keysize 2048 \
        -validity ${VALIDITY_DAYS} -keyalg RSA \
        -dname "CN=${DOMAIN_HOST}" -keypass ${PASSWORD} -storepass $PASSWORD \
        -keystore entdiy-nat-server.jks

echo 生成服务端自签名证书...
keytool -export -alias entdiy-nat-server -keystore entdiy-nat-server.jks \
        -storepass ${PASSWORD} -file entdiy-nat-server.cer

echo 生成客户端的密钥对和证书仓库
keytool -genkey -alias entdiy-nat-client -keysize 2048 \
        -validity ${VALIDITY_DAYS}  -keyalg RSA \
        -dname "CN=${DOMAIN_HOST}" -keypass ${PASSWORD}  -storepass ${PASSWORD} \
        -keystore entdiy-nat-client.jks

echo 将服务端证书导入到客户端的证书仓库中...
keytool -import -trustcacerts -alias entdiy-nat-server -file entdiy-nat-server.cer \
        -storepass ${PASSWORD} -keystore entdiy-nat-client.jks

echo 生成客户端自签名证书...
keytool -export -alias entdiy-nat-client -keystore entdiy-nat-client.jks \
        -storepass ${PASSWORD} -file entdiy-nat-client.cer

echo 将客户端的自签名证书导入到服务端的信任证书仓库中...
keytool -import -trustcacerts -alias entdiy-nat-client -file entdiy-nat-client.cer \
        -storepass ${PASSWORD} -keystore entdiy-nat-server.jks

ls -lh ${BASE_DIR}

