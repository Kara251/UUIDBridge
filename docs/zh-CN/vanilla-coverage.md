# 原版覆盖范围

本页定义“原版完全迁移”的验收边界：UUIDBridge 迁移 Minecraft 1.21.1
原版会持久化到磁盘的玩家 UUID 身份引用；不迁移玩家名，也不修改外部账号服务数据。

## 覆盖文件

- 服务器根目录：`whitelist.json`、`ops.json`、`banned-players.json`、
  `usercache.json`。
- 玩家文件：`world/playerdata/*.dat`、`world/advancements/*.json`、
  `world/stats/*.json`，并重命名以 UUID 为文件名的玩家文件。
- 世界根：`world/level.dat`、`world/level.dat_old`。
- 保存数据：`world/data/*.dat`，包括 scoreboard、raid、map、command storage
  等原版 saved data。
- 区域文件：`region/*.mca`、`entities/*.mca`、`poi/*.mca`，以及所有维度目录
  下的同类文件，例如 `DIM-1/region/*.mca`、`DIM1/entities/*.mca`。

## 覆盖 UUID 形态

- 标准字符串 UUID。
- 无横杠字符串 UUID。
- NBT `int[4]` UUID。
- NBT `long[2]` UUID。
- `UUIDMost` / `UUIDLeast` 及同类 `*Most` / `*Least` long pair。

## 覆盖原版语义

- 实体、方块实体和玩家 NBT 中的 owner、trusted player、anger/love cause、
  conversion player、projectile owner/thrower、leash 相关 UUID。
- 狼、猫、鹦鹉、马、狐狸等可驯服或信任实体的归属数据。
- 村民 gossip target。
- BossBar player list。
- raid heroes / raid saved data。
- scoreboard 中以 UUID 形式保存的值。
- 玩家头颅 owner profile 和 1.21 item component profile 中的 UUID。
- Brain memories 中以 UUID 保存的玩家引用。
- 单人存档 `level.dat Data.Player`：只有一个映射时会自动复制到
  `playerdata/<targetUuid>.dat`；多映射时可用 `--singleplayer-name <name>` 明确指定。

## 明确不做

- 不改 scoreboard 队伍成员名、分数 owner 名等玩家名字段。online/offline 迁移不改变玩家名。
- 不改 `banned-ips.json`，它不是玩家 UUID 数据。
- 不扫描日志、崩溃报告、备份、mods、build、`.git`。
- 不写 SQLite、LevelDB、H2、MySQL 或其他数据库。
- 不对任意长文本里的 UUID 做模糊替换；只迁移可证明是 UUID 身份引用的精确值。
