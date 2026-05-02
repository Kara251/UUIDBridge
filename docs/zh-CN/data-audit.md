# 玩家数据迁移审计

UUIDBridge 应迁移的是身份引用，不是玩家名。online-mode 和 offline-mode
通常不改变可见玩家名，所以只以玩家名为 key 的数据默认应报告，不应改写。

## 当前已覆盖

- 玩家文件名：`playerdata`、`advancements`、`stats`。
- 服务器身份列表：`whitelist.json`、`ops.json`、`banned-players.json`、
  `usercache.json`。
- 世界 NBT 与 region 数据：`level.dat`、`data/*.dat`、`region/*.mca`、
  `entities/*.mca`。
- 常见 UUID 形态：标准字符串、无横杠字符串、int array、long array、
  `UUIDMost` / `UUIDLeast`。
- 实体、方块实体、玩家数据和模组 NBT 中的归属引用：owner、trusted players、
  anger/love cause、conversion player、projectile owner/thrower、leash、村民
  gossip、BossBar players、袭击保存数据、scoreboard UUID 值、玩家头颅 profile、
  1.21 item component profile、Brain memories、Touhou Little Maid 风格
  `owner_uuid`。
- 单人存档转服务器：只有一个映射时自动把 `level.dat` 的 `Data.Player` 复制到
  `playerdata/<targetUuid>.dat`；多映射时使用 `--singleplayer-name <name>` 指定。
- `uuidbridge/targets.json` 声明的额外 JSON、NBT 或 region 文件。

## 重要缺口

- 还没有真实模组专用 adapter：FTB Teams、FTB Chunks、Open Parties and Claims、
  LuckPerms 存储、经济、任务、坟墓、商店、家、传送点、背包归属等都需要真实样本。
- SQLite、H2、MySQL dump、LevelDB 和模组私有事务文件不能通用改写，必须有格式
  专用 adapter 和夹具。
- scoreboard 里以玩家名为 key 的分数和队伍成员默认不改。这对 UUID 模式迁移是
  正确的；如果以后做改名迁移，那是另一套规则。
- 通用 binary 替换只保留为已知 adapter 的内部 fallback；公开 `binary` 目标不接受。
- region 现在仍会读入整个 `.mca` 文件后重写变化 chunk。下一步性能优化应做
  sector-level writer。

## 下一批应该补的玩家数据

- 权限、领地、队伍、经济、任务、坟墓、商店、家、传送点、背包等真实服务器夹具。
- 对不支持数据库做 report-only 检测，尽可能标出路径和可能的模组名。

## 应该收敛或删除的功能

- Mojang 联网查询已移除。offline-to-online 必须提供显式映射文件，保证结果可复现、
  可审计。
- 公开 `binary` 目标风险偏高，alpha 不开放。除非有真实样本证明需要，否则继续
  不可用。
- 没接入真实迁移路径的占位类型和选项应删除，不该留在主线里装样子。
