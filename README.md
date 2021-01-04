## 项目简介

类似Ngrok，基于Netty实现的NAT内网穿透访问软件。

**entdiy-nat-common** - 公共组件类代码模块

**entdiy-nat-server** - Server服务端代码模块

**entdiy-nat-client** - Client客户端代码模块

**devops** - 提供一套简单的脚本和配置实现软件快速部署运行

### 项目托管同步更新GIT资源库：

**https://github.com/xautlx/entdiy-nat**

**https://gitee.com/xautlx/entdiy-nat**

## 快速开始

特别提示：以下相关命令基于Mac或Linux等环境，Windows系统请参考相关脚本自行转换命令执行。

* 获取整个项目，并进入项目主目录

* 运行构建：
~~~shell script
mvn install
~~~

* 查阅 devops/entdiy-nat-server/application-server.yml 中相关配置参数，可按照注释含义按需调整。
如果要体验实际效果可把entdiy-nat-server目录整体复制到公网服务器，简单体验也可以在当前机器直接运行。
启动运行Server端：
~~~shell script
./devops/entdiy-nat-server/nat-server-cli.sh restart
~~~

* 查阅 devops/entdiy-nat-client/application-client.yml 中相关配置参数，
默认穿透访问当前主机的22端口SSH服务和3306MySQL服务，可按照注释含义按需调整。
启动运行Client端：
~~~shell script
./devops/entdiy-nat-client/nat-client-cli.sh restart
~~~

* TCP协议穿透访问测试，以 122 端口SSH访问穿透内容主机或者用工具以 13306 端口访问MySQL：
~~~shell script
ssh -p 122 root@127.0.0.1
~~~

## 功能列表

已实现功能列表：

* **TCP协议穿透** - TCP协议网络访问穿透，如常见的SSH、MySQL等，当然不局限于这些服务，只要是TCP协议理论均可穿透访问
* **连接池处理** - Server端与Client端的Proxy Channel采用简化连接池实现，提升穿透连接的初始化连接速度和效率

## 路线计划

待实现功能列表：

* **HTTP/HTTPS协议穿透** - 典型网页服务HTTP/HTTPS协议穿透支持
* **Server端Tunnel定义控制** - 典型的内网穿透都是由Client定义隧道，但是对于一些业务场景需要由Server进行全局的各Client端的隧道定义实现灵活的Server端管控支持
* **SSL证书支持** - 引入SSL证书支持，提升穿透访问数据安全性
* **Client重连机制** - 由于网络异常或Server端重启，需要Client重连机制来实现连接中断后自动重连Server
