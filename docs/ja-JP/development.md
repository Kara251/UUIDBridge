# 開発ガイド

UUIDBridge は Minecraft 1.21.1 を対象とし、JDK 21 が必要です。

## よく使うコマンド

```sh
./gradlew test
./gradlew build
./gradlew smokeServerAll
```

Gradle Wrapper を使うため、グローバル Gradle は不要です。

## ビルド成果物

インストール可能な jar は以下に配置されます。

- `build/distributions/mods/fabric/`
- `build/distributions/mods/forge/`
- `build/distributions/mods/neoforge/`

## Smoke Test

`smokeServerAll` は Fabric、Forge、NeoForge の dedicated server を起動し、
`uuidbridge status` を実行してから停止します。通常の単体テストより遅いため、
既定の `build` には接続していません。

Forge と NeoForge の smoke test は本番 server installer と remapped
UUIDBridge jar を使います。Fabric は開発用 `runServer` と一時サーバー
ディレクトリを使います。
