# 运维指南

UUIDBridge 面向面板服环境设计。管理员通常只有控制台、文件管理器和重启按钮，
不一定有完整 shell。

## 标准流程

1. 上传对应加载器的 Fabric、Forge 或 NeoForge jar。
2. 启动一次服务器，在面板控制台运行 `uuidbridge status`。
3. 如果是从离线模式迁回正版验证模式，把映射文件上传到服务器根目录。
4. 运行 `uuidbridge scan <online-to-offline|offline-to-online>`。
5. 运行 `uuidbridge plan <direction> [--mapping <file>] [--allow-network]`。
6. 运行 `uuidbridge apply <planId> --confirm`。
7. 重启服务器。UUIDBridge 会在正常进入世界前执行 pending 计划。

启动迁移时不要强制停止服务器，除非面板已经确认卡死。apply 失败时，
UUIDBridge 会先尝试根据备份 manifest 自动恢复已改文件，再阻止启动。

## 回滚流程

撤销一次已完成迁移：

```sh
uuidbridge rollback <planId> --confirm
```

标记 rollback pending 后重启服务器。UUIDBridge 会从
`uuidbridge/backups/<planId>/manifest.json` 恢复文件；覆盖当前文件前，会先把
当前文件保存到 `uuidbridge/backups/<planId>/rollback-current/`。

## 映射文件

CSV：

```csv
name,onlineUuid,offlineUuid
Alice,11111111-2222-3333-4444-555555555555,aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee
```

JSON：

```json
[
  {
    "name": "Alice",
    "onlineUuid": "11111111-2222-3333-4444-555555555555",
    "offlineUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
  }
]
```

从正版验证模式迁到离线模式时，`offlineUuid` 可以省略，因为 UUIDBridge 能根据
玩家名计算离线 UUID。从离线模式迁回正版验证模式时，建议提供映射文件；不要
依赖联网查询。

## 已覆盖数据

- `playerdata/*.dat`
- `advancements/*.json`
- `stats/*.json`
- `whitelist.json`、`ops.json`、`banned-players.json`、`usercache.json`
- 世界 `region`、`entities`、`data`、`level.dat` NBT 文件
- 实体 owner、村民 gossip、BossBar 玩家列表、袭击保存数据、scoreboard 中的
  UUID 值等原版身份引用
- `uuidbridge/targets.json` 声明的额外 JSON、NBT、region 或显式 binary 目标

scoreboard 中的玩家名默认不改。online/offline UUID 迁移改变的是 UUID 身份，
不是玩家名。

## 额外目标

当领地、队伍、权限、背包等模组把归属数据存放在标准世界文件之外时，可以创建
`uuidbridge/targets.json`。格式见 [额外迁移目标](compat/targets.md)。

```sh
uuidbridge scan online-to-offline --targets uuidbridge/targets.json
uuidbridge plan online-to-offline --targets uuidbridge/targets.json
```

SQLite、LevelDB 和其他事务型数据库本阶段不直接写入。没有专用 adapter 前，
不要把数据库文件加入迁移目标。

## 单人存档 Player 标签

单人存档转服务器时，`level.dat` 里可能有 `Data.Player`，而不是单独的
`playerdata/<uuid>.dat`。

```sh
uuidbridge plan online-to-offline --singleplayer-name Alice
```

如果目标 `playerdata/<targetUuid>.dat` 不存在，UUIDBridge 会在启动迁移时把
嵌入的 Player 标签复制过去。原 `Data.Player` 会保留，方便回滚和人工检查。

## 运行时文件

运行时文件写在服务器根目录的 `uuidbridge/`：

- `plans/<planId>.json`
- `pending.json`
- `migration.lock`
- `reports/<planId>.json`
- `reports/<planId>-rollback.json`
- `backups/<planId>/manifest.json`
- `backups/<planId>/rollback-current/`

使用 `uuidbridge status` 查看 pending、最近报告、lock 和备份 manifest 状态。
