以下是在 Ubuntu Server 上快速安装 ThingsBoard Edge 并连接到服务器的命令列表。

**注意：** OpenJDK 25 需要 Ubuntu 22.04 LTS 或更高版本，不支持更早版本（20.04 及以下）。

#### 步骤 1. 安装 Java 25（OpenJDK）
ThingsBoard 服务运行在 Java 25 上。请按以下步骤安装 OpenJDK 25：

```bash
sudo apt update && sudo apt install openjdk-25-jdk
{:copy-code}
```

将操作系统的默认 Java 配置为 OpenJDK 25，可通过以下命令配置默认版本：

```bash
sudo update-alternatives --config java
{:copy-code}
```

要检查系统上已安装的 Java 版本，使用以下命令：

```bash
java -version
{:copy-code}
```

预期结果：

```text
openjdk version "25.x.xx"
OpenJDK Runtime Environment (...)
OpenJDK 64-Bit Server VM (build ...)
```

#### 步骤 2. 配置 ThingsBoard Edge 数据库

ThingsBoard Edge 支持 **SQL** 和 **混合（hybrid）** 两种数据库配置。
本指南使用 **SQL** 数据库。
关于混合配置的更多细节，请参考 <a href="https://allshen.org/docs/user-guide/install/edge/deb-installation/#step-2-configure-the-thingsboard-edge-database" target="_blank">ThingsBoard 官方安装文档</a>。

运行以下命令安装 PostgreSQL 数据库：

```bash
# 自动化仓库配置：
sudo apt install -y postgresql-common
sudo /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh

# 安装并启动 postgresql 服务：
sudo apt update
sudo apt -y install postgresql-16
sudo service postgresql start
{:copy-code}
```

PostgreSQL 安装完成后，建议为 PostgreSQL 主用户设置密码。

以下命令会切换到 PostgreSQL 用户并直接在 PostgreSQL 中设置密码。

```bash
sudo -u postgres psql -c "\password"
{:copy-code}
```

然后输入并确认密码。

最后，运行以下命令创建一个名为 tb_edge 的新 PostgreSQL 数据库：

```bash
echo "CREATE DATABASE tb_edge;" | psql -U postgres -d postgres -h 127.0.0.1 -W
{:copy-code}
```

#### 步骤 3. 安装 ThingsBoard Edge 服务
下载安装包：

```bash
wget https://github.com/thingsboard/thingsboard-edge/releases/download/v${TB_EDGE_TAG}/tb-edge-${TB_EDGE_TAG}.deb
{:copy-code}
```

进入下载目录，安装 ThingsBoard Edge 服务：

```bash
sudo dpkg -i tb-edge-${TB_EDGE_TAG}.deb
{:copy-code}
```

#### 步骤 4. 配置 ThingsBoard Edge
可使用以下命令自动将指定的配置值写入配置文件：

```bash
sudo sh -c 'cat <<EOL >> /etc/tb-edge/conf/tb-edge.conf
export CLOUD_ROUTING_KEY=${CLOUD_ROUTING_KEY}
export CLOUD_ROUTING_SECRET=${CLOUD_ROUTING_SECRET}
export CLOUD_RPC_HOST=${BASE_URL}
export CLOUD_RPC_PORT=${CLOUD_RPC_PORT}
export CLOUD_RPC_SSL_ENABLED=${CLOUD_RPC_SSL_ENABLED}
EOL'
{:copy-code}
```

##### [可选] 配置 PostgreSQL 连接
如果修改过 PostgreSQL 的默认数据源设置，请使用以下命令：

```bash
sudo sh -c 'cat <<EOL >> /etc/tb-edge/conf/tb-edge.conf
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tb_edge
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=<PUT_YOUR_POSTGRESQL_PASSWORD_HERE>
EOL'
{:copy-code}
```

* **PUT_YOUR_POSTGRESQL_PASSWORD_HERE**：替换为你实际的 **PostgreSQL 用户密码**。

##### [可选] 修改绑定端口
如果 ThingsBoard Edge 与 ThingsBoard Server 运行在同一台机器上，需要修改端口配置以避免两个服务间的端口冲突。

请执行以下命令更新 ThingsBoard Edge 配置文件（**/etc/tb-edge/conf/tb-edge.conf**）：

```bash
sudo sh -c 'cat <<EOL >> /etc/tb-edge/conf/tb-edge.conf
export HTTP_BIND_PORT=18080
export MQTT_BIND_PORT=11883
export COAP_BIND_PORT=15683
export LWM2M_ENABLED=false
export SNMP_ENABLED=false
EOL'
{:copy-code}
```

请确认端口 **18080**、**11883** 和 **15683–15688** 未被其他应用占用。

#### 步骤 5. 运行安装脚本

ThingsBoard Edge 安装并配置完成后，请执行以下安装脚本：

```bash
sudo /usr/share/tb-edge/bin/install/install.sh
{:copy-code}
```

#### 步骤 6. 启动 ThingsBoard Edge 服务

```bash
sudo service tb-edge start
{:copy-code}
```

#### 步骤 7. 打开 ThingsBoard Edge 界面

Edge 服务启动后，访问 http://localhost:8080 打开 Edge 的 Web 界面；如果上一步修改了 HTTP 绑定端口，则访问 http://localhost:18080。

使用来自本地 ThingsBoard Server 或 **ThingsBoard Live Demo** 的 **租户账号** 登录。
