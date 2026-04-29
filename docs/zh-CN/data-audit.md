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
  anger/love cause、村民 gossip、BossBar players、袭击保存数据、scoreboard
  UUID 值、Touhou Little Maid 风格 `owner_uuid`。
- 单人存档转服务器：显式 `--singleplayer-name <name>` 时，可以把 `level.dat`
  的 `Data.Player` 复制到 `playerdata/<targetUuid>.dat`。
- `uuidbridge/targets.json` 声明的额外 JSON、NBT、region 或显式 binary 文件。

## 重要缺口

- 还没有真实模组专用 adapter：FTB Teams、FTB Chunks、Open Parties and Claims、
  LuckPerms 存储、经济、任务、坟墓、商店、家、传送点、背包归属等都需要真实样本。
- SQLite、H2、MySQL dump、LevelDB 和模组私有事务文件不能通用改写，必须有格式
  专用 adapter 和夹具。
- scoreboard 里以玩家名为 key 的分数和队伍成员默认不改。这对 UUID 模式迁移是
  正确的；如果以后做改名迁移，那是另一套规则。
- 通用 binary 替换不能宽泛扫描，只能显式 opt-in。
- region 现在仍会读入整个 `.mca` 文件后重写变化 chunk。下一步性能优化应做
  sector-level writer。

## 下一批应该补的玩家数据

- 玩家头颅 owner profile 和 item component 里的玩家 UUID。
- Brain memories 与实体专用引用：liked player、angry target、trusted UUID list、
  projectile owner/thrower、leash data。
- `data/map_*.dat` 与 command storage 夹具，因为数据包和模组可能把玩家 UUID
  存在那里。
- 权限、领地、队伍、经济、任务、坟墓、商店、家、传送点、背包等真实服务器夹具。
- 对不支持数据库做 report-only 检测，尽可能标出路径和可能的模组名。

## 应该收敛或删除的功能

- Mojang 联网查询不是安全迁移的必要功能。映射文件更可复现；联网查询应继续默认
  关闭，alpha 甚至可以移除。
- 公开 `binary` 目标风险偏高。除非有真实样本证明需要，否则应保持显式 opt-in，
  或从公开文档里移除。
- 没接入真实迁移路径的占位类型和选项应删除，不该留在主线里装样子。
