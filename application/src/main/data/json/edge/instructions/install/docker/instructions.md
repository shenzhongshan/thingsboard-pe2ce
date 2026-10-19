以下是使用 docker compose 快速安装 ThingsBoard Edge 并连接到服务器的命令列表。

#### 前置条件

安装 <a href="https://docs.docker.com/engine/install/" target="_blank"> Docker CE</a> 和 <a href="https://docs.docker.com/compose/install/" target="_blank"> Docker Compose</a>。

#### 步骤 1. 创建 ThingsBoard Edge 的 Docker Compose 文件

ThingsBoard Edge 支持 **内存队列** 和 **Kafka** 两种消息存储及 ThingsBoard 服务间通信方式。
同时支持 **SQL** 和 **混合（hybrid）** 两种数据库配置。
本指南使用 **内存队列** 和 **SQL** 数据库。
关于混合配置的更多细节，请参考 <a href="https://allshen.org/docs/user-guide/install/edge/docker/#step-2-choose-queue-andor-database-services" target="_blank">ThingsBoard 官方安装文档</a>。

现在，为 ThingsBoard Edge 服务创建一个 Docker Compose 文件：

```bash
nano docker-compose.yml
{:copy-code}
```

将以下内容添加到 yml 文件中：

```bash
services:
  mytbedge:
    restart: always
    image: "thingsboard/tb-edge:${TB_EDGE_VERSION}"
    ports:
      - "8080:8080"
      - "1883:1883"
      - "5683-5688:5683-5688/udp"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tb-edge
      CLOUD_ROUTING_KEY: ${CLOUD_ROUTING_KEY}
      CLOUD_ROUTING_SECRET: ${CLOUD_ROUTING_SECRET}
      CLOUD_RPC_HOST: ${BASE_URL}
      CLOUD_RPC_PORT: ${CLOUD_RPC_PORT}
      CLOUD_RPC_SSL_ENABLED: ${CLOUD_RPC_SSL_ENABLED}
    ${EXTRA_HOSTS}
    volumes:
      - tb-edge-data:/data
      - tb-edge-logs:/var/log/tb-edge
  postgres:
    restart: always
    image: "postgres:16"
    ports:
      - "5432"
    environment:
      POSTGRES_DB: tb-edge
      POSTGRES_PASSWORD: postgres
    volumes:
      - tb-edge-postgres-data:/var/lib/postgresql/data

volumes:
  tb-edge-data:
    name: tb-edge-data
  tb-edge-logs:
    name: tb-edge-logs
  tb-edge-postgres-data:
    name: tb-edge-postgres-data
{:copy-code}
```

##### [可选] 修改绑定端口
如果 ThingsBoard Edge 与 ThingsBoard Server 运行在同一台机器上，需要修改端口配置以避免两个服务间的端口冲突。

请确认端口 **18080**、**11883** 和 **15683–15688** 未被其他应用占用。

然后，相应地修改 `docker-compose.yml` 文件中的端口配置：

```bash
sed -i 's/8080:8080/18080:8080/; s/1883:1883/11883:1883/; s/5683-5688:5683-5688\/udp/15683-15688:5683-5688\/udp/' docker-compose.yml
{:copy-code}
```

#### 步骤 2. 启动 ThingsBoard Edge
进入包含 `docker-compose.yml` 文件的目录，运行以下命令启动 ThingsBoard Edge 服务：

```bash
docker compose up -d && docker compose logs -f mytbedge
{:copy-code}
```

#### 步骤 3. 打开 ThingsBoard Edge 界面

Edge 服务启动后，访问 http://localhost:8080 打开 Edge 的 Web 界面；如果上一步修改了 HTTP 绑定端口，则访问 http://localhost:18080。

使用来自本地 ThingsBoard Server 或 **ThingsBoard Live Demo** 的 **租户账号** 登录。
