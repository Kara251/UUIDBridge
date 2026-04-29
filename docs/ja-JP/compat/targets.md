# 追加移行ターゲット

標準の Minecraft ワールドファイル外にある mod データを扱う場合、
`uuidbridge/targets.json` を使います。既定ではサーバールートからの相対パスです。
`world:` を付けると、現在のワールドディレクトリからの相対パスになります。

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

対応形式：

- `auto`：拡張子から選択します。
- `json`：正確に一致する UUID 文字列値を書き換え、未知フィールドは保持します。
- `nbt-gzip`：圧縮 NBT を書き換えます。
- `nbt-plain`：非圧縮 NBT を書き換えます。
- `region`：`.mca` chunk を書き換えます。
- `binary`：明示的に指定されたファイルだけで UUID byte/string を正確に置換します。

パスはサーバーまたはワールドディレクトリの外へ出られません。広範囲スキャンでは
`uuidbridge/backups`、ログ、クラッシュレポート、`.git`、build 出力、`mods`
ディレクトリを除外します。

SQLite、LevelDB などのデータベースファイルはこの段階では書き換えません。
実データと専用 adapter が揃ってから扱います。
