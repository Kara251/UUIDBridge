# 变更记录

## Unreleased

- 初始化 UUIDBridge，采用 Minecraft 1.21.1 多加载器结构。
- 增加迁移集成夹具、备份 manifest、失败诊断和 dedicated server smoke task。
- 增加 pending apply/rollback 恢复工作流、控制文件原子写入、失败自动回滚和手动
  rollback 报告。
- 增加身份引用覆盖报告、语义 JSON/NBT adapter、额外模组目标文件和单人存档
  `Data.Player` 提取。
- 从公开仓库移除代理内部指令文件。
- 将文档整理为中文、英文、日文三语目录，并把变更记录移入 `docs/`。
- 删除未接入真实迁移流程的 `IdentityReference` 占位类型。
- 移除 `targets.json` 对公开 `binary` 条目的支持。
- 移除 Mojang 联网查询；offline-to-online 迁移必须提供显式映射文件。
