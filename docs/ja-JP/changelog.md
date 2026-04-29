# 変更履歴

## Unreleased

- Minecraft 1.21.1 向けの multi-loader mod として UUIDBridge を初期化。
- 移行 integration fixture、backup manifest、失敗診断、dedicated server smoke
  task を追加。
- pending apply/rollback 復旧フロー、制御ファイルの atomic write、apply 失敗時の
  自動 rollback、手動 rollback report を追加。
- identity coverage report、semantic JSON/NBT adapter、追加 mod target file、
  singleplayer `Data.Player` 抽出を追加。
- 公開リポジトリから agent 内部指示ファイルを削除。
- ドキュメントを中国語、英語、日本語の言語別ディレクトリへ整理し、変更履歴を
  `docs/` へ移動。
- 実際の移行経路に接続されていない `IdentityReference` placeholder type を削除。
- `targets.json` の公開 `binary` entry support を削除。
