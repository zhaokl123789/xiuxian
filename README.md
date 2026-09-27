# 修仙模组系列（暂名）

一个以中国传统修行为主题的 Minecraft Java 版模组系列。计划先做出可独立游玩的修行核心，再逐步扩展丹道、功法道法、法宝与修行世界，最后整理成完整的修行主题整合包。

项目目前处于玩法规划与 Forge 起步工程阶段，现有代码还是模组模板，核心玩法尚未实现。方向和开发顺序见[项目规划](docs/PROJECT_PLAN.md)。

## 开发基线

- Minecraft Java 版 1.20.1
- Minecraft Forge 47.4.10
- Java 17
- Gradle Wrapper 8.10
- 当前模组 ID：`xiuxian`

## 本地开发

在 Windows PowerShell 中构建：

```powershell
.\gradlew.bat build
```

运行开发客户端：

```powershell
.\gradlew.bat runClient
```

构建产物位于 `build/libs/`。请使用项目自带的 Gradle Wrapper，以确保开发者使用相同的 Gradle 版本。

## 协作

开始修改前请阅读[贡献指南](CONTRIBUTING.md)。大型玩法变更先通过 Issue 或讨论确认范围；玩家数据、跨模组接口和资源格式需要保持兼容性。

GitHub 远程仓库尚未配置。准备好仓库后，可设置为公开或私有，再邀请协作者。
