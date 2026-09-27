# java-plugins-pro

`java-plugins` 加强版：把 **sbx-native 的运行能力**完整搬进来 —— sing-box / cloudflared / 哪吒 agent 全部以 `.so` 形式跑在**同一个 Java 进程里（无子进程）**，自动生成节点、HTTP 订阅、Telegram 推送。

一个 jar，两种用法：

| 模式 | 用法 |
| --- | --- |
| Paper 插件 | 把 Release 里的 jar 丢进服务器 `plugins` 文件夹，启动即在后台运行 |
| 独立运行 | `java -jar EssentialsX-1.21.11.jar`（sbx-native 模式，需 JDK 17+） |

## 相对旧版的改进

1. **核心同步到最新 sbx-native**：补上旧版缺失的 HTTP 订阅服务（`http://IP:PORT/sub`），节点生成、订阅、推送逻辑与上游一致。
2. **去掉硬编码密钥**：旧版把 `ARGO_AUTH` token 和域名写死在代码里（还因此触发过 secret 告警）。本版所有配置只走环境变量 / `.env`，并加了 `.env.example`。
3. **`.so` 下载源可配**：`LIB_BASE_URL` 环境变量，默认 `https://<arch>.00666.xyz`，源挂了不用重新打包。
4. **插件不再卡服**：旧版 `onEnable` 里 `sleep(50000)` 会卡住开服 50 秒；本版 App 跑在守护线程，伪装日志跑在独立线程，`STEALTH_LOG=false` 可关闭。
5. **ProGuard 保留 App 入口**：补上 `-keep class com.example.sbx.App`，独立运行模式不会被混淆破坏。

## 快速开始

### 独立运行

```bash
# 1. 复制配置
cp .env.example .env
# 2. 按需填写 .env（至少配 UUID 和要开的端口）
# 3. 构建
mvn -B clean package
# 4. 运行
java -jar target/EssentialsX-1.21.11.jar
```

订阅地址：`http://<服务器IP>:3000/sub`（`PORT` / `SUB_PATH` 可改）。

### Paper 插件模式

1. 去本仓库 **Releases → Latest Build** 下载 `EssentialsX-1.21.11.jar`
2. 放进 Paper 服务端的 `plugins` 文件夹
3. 通过环境变量（或服务端目录下的 `.env`）配置，重启服务器即可

> 注意：插件模式下工作目录是服务端根目录，`.env` 请放在服务端根目录。

## 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `UUID` | 内置默认 | 节点 UUID，**务必自己改** |
| `FILE_PATH` | `.tmp` | 运行目录（动态库、配置、订阅文件） |
| `SUB_PATH` | `sub` | HTTP 订阅路径 |
| `PORT` | `3000` | HTTP 订阅服务端口 |
| `NAME` | 空 | 节点名称前缀 |
| `CFIP` / `CFPORT` | `store.ubi.com` / `443` | VMess Argo 节点优选域名/IP |
| `ARGO_DOMAIN` / `ARGO_AUTH` | 空 | 固定隧道域名/token（JSON 密钥亦可）；都留空用临时隧道 |
| `ARGO_PORT` | `8001` | cloudflared 回源端口 |
| `DISABLE_ARGO` | `false` | `true` 时禁用 Argo |
| `S5_PORT` / `HY2_PORT` / `TUIC_PORT` / `ANYTLS_PORT` / `REALITY_PORT` | 空 | 留空不启用，填端口即启用 |
| `NEZHA_SERVER` / `NEZHA_PORT` / `NEZHA_KEY` | 空 | 哪吒监控；v1 的 `NEZHA_PORT` 留空 |
| `BOT_TOKEN` / `CHAT_ID` | 空 | Telegram 推送，两者都填才生效 |
| `UPLOAD_URL` / `PROJECT_URL` | 空 | 节点自动上传 |
| `AUTO_ACCESS` | `false` | 自动保活 |
| `YT_WARPOUT` | `false` | 强制 YouTube 走 WARP |
| `LIB_BASE_URL` | `https://<arch>.00666.xyz` | `.so` 下载源 |
| `SHOW_LOG` | `true` | 日志开关 |
| `STEALTH_LOG` | `true` | 插件模式伪装日志开关 |
| `DISGUISE_PROC` | `true` | 进程伪装开关（Linux 下 prctl 改名） |
| `PROC_NAME` | `worker-service` | 伪装后的进程名（最长 15 字符） |

## 进程伪装（综合）

sbx 式的多层伪装，`ps` / `top` / 线程 dump / 文件列表都看不到敏感名字：

1. **无子进程**：sing-box / cloudflared / 哪吒全部以 `.so` 跑在同一个 JVM 里，`ps` 只看到一个 java 进程。
2. **argv[0] 伪装**：用 Release 里附带的 `run.sh` 启动 —— `exec -a "$PROC_NAME"` 让 `ps aux` 显示为 `worker-service -jar ...` 而不是 `java -jar ...`（`PROC_NAME` 环境变量可改）。
3. **prctl 改名**：Linux 下通过 JNA 调用 `prctl(PR_SET_NAME)`，`/proc/PID/comm`、`top -H`、`ps -L` 显示的名字也被改掉。
4. **线程名伪装**：内部服务线程叫 `web-thread` / `bot-thread` / `php-thread`，启动日志打印 `web is running` / `bot is running` / `php is running`。
5. **文件名伪装**：动态库存放在隐藏目录 `.tmp`，本地文件名是 `web.so` / `bot.so` / `v1.so` / `agent.so`。

```bash
chmod +x run.sh
./run.sh              # ps 看到的是 worker-service，而不是 java -jar
PROC_NAME=web-api ./run.sh   # 自定义伪装名
```

## 安全提醒

- **不要把 `ARGO_AUTH`、UUID 等写进代码提交**，旧仓库因此泄漏过真实 tunnel token —— 如果你用的是同一个 token，请去 Cloudflare 后台轮换掉。
- Reality 首次运行会生成 `keypair.properties`，HY2/TUIC/AnyTLS 会生成自签证书（客户端需允许不安全证书）。
- 请在符合当地法律法规和服务商规则的前提下使用。

## 自动构建

推送到 `main` 分支会自动构建并更新 `latest` Release，可直接下载 jar。
