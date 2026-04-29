# 开发指南

UUIDBridge 目标版本是 Minecraft 1.21.1，需要 JDK 21。

## 常用命令

```sh
./gradlew test
./gradlew build
./gradlew smokeServerAll
```

项目使用 Gradle Wrapper，不依赖全局 Gradle。

## 构建产物

可安装 jar 会输出到：

- `build/distributions/mods/fabric/`
- `build/distributions/mods/forge/`
- `build/distributions/mods/neoforge/`

## Smoke Test

`smokeServerAll` 会分别启动 Fabric、Forge、NeoForge dedicated server，运行
`uuidbridge status`，然后停止服务器。这个任务比普通单元测试慢，所以没有接入
默认 `build`。

Forge 和 NeoForge smoke test 使用正式服务端 installer 和 remapped
UUIDBridge jar。Fabric 使用开发环境 `runServer` 和临时服务端目录。
