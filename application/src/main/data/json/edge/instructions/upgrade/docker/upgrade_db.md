为 ThingsBoard Edge 升级流程创建一个 docker compose 文件：

```bash
> docker-compose-upgrade.yml && nano docker-compose-upgrade.yml
{:copy-code}
```

将以下内容添加到 yml 文件中：

```bash
services:
  mytbedge:
    restart: on-failure
    image: "thingsboard/tb-edge:${TB_EDGE_VERSION}"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tb-edge
    volumes:
      - tb-edge-data:/data
      - tb-edge-logs:/var/log/tb-edge
    entrypoint: upgrade-tb-edge.sh
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

执行以下命令启动升级流程：

```bash
docker compose -f docker-compose-upgrade.yml up --abort-on-container-exit
{:copy-code}
```