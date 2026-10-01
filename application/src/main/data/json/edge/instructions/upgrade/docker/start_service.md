修改 ThingsBoard Edge 的"主"docker compose 文件（`docker-compose.yml`），更新镜像版本：
```bash
nano docker-compose.yml
{:copy-code}
```

```text
services:
    mytbedge:
        restart: always
        image: "thingsboard/tb-edge:${TB_EDGE_VERSION}"
...
```

请确认镜像已设置为 **tb-edge:${TB_EDGE_VERSION}**。
执行以下命令直接启动该 docker compose：

```bash
docker compose up -d
docker compose logs -f mytbedge
{:copy-code}
```
