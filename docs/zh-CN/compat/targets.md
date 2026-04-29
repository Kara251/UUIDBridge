# 额外迁移目标

当模组数据不在标准 Minecraft 世界文件里时，使用 `uuidbridge/targets.json`
声明额外扫描目标。默认路径相对服务器根目录；使用 `world:` 前缀时相对当前
世界目录。

```json
{
  "include": [
    {
      "path": "config/claims/*.json",
      "format": "json"
    },
    {
      "path": "world:data/mod_backpacks.dat",
      "format": "nbt-gzip"
    }
  ],
  "exclude": [
    "config/claims/archive/**"
  ]
}
```

支持格式：

- `auto`：根据扩展名选择。
- `json`：迁移精确 UUID 字符串值，并保留未知字段。
- `nbt-gzip`：迁移压缩 NBT。
- `nbt-plain`：迁移未压缩 NBT。
- `region`：迁移 `.mca` chunk。
- `binary`：只对显式包含文件做精确 UUID 字节或字符串替换。

路径不能逃出服务器目录或世界目录。宽泛扫描默认排除 `uuidbridge/backups`、
日志、崩溃报告、`.git`、构建输出和 `mods` 目录。

SQLite、LevelDB 等数据库文件本阶段不写入。必须等真实样本和专用 adapter
准备好后再做。
