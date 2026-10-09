# 修行项目可选整合包

适用 Minecraft 1.20.1、Forge 47.4.10、Java 17。当前整合 FTB Quests、FTB Teams、FTB Library、Architectury API、Xaero 世界地图与小地图；XaeroLib 1.7.3 由地图模组内置。

这些模组在运行时独立安装，修行核心 JAR 无需它们也可以游玩。FTB 任务是配置文件，不改变境界、身份、血量、奖励或建筑生成。

## 当前项目启动

安装器需要 Python 3.11 或更新版本；启动脚本优先使用本机 Codex 自带的 Python。

双击项目根目录的 `start-pack.cmd`，或运行：

```powershell
.\tools\start_pack.ps1
```

首次安装需要联网，必要时使用 `-Proxy http://127.0.0.1:7890`。安装完成后，直接运行 `.\gradlew.bat runClient` 也会自动启用整合包。`-PwithPack=false` 可临时关闭。第三方模组用 ForgeGradle 重映射后进入开发运行环境，不把发行版 JAR 直接放入开发目录的 `run/mods`。

## 普通启动器或服务器

本地生成的 `build/pack/client` 与 `build/pack/server` 分别包含需要的 `mods` 和 `config`。安装 Forge 后，将相应文件夹安装到该实例的游戏目录；服务器端不安装 Xaero 地图。直接复制文件夹时，按键仍使用该实例已有配置；下面的安装器还会处理按键冲突，并移除旧版整合包添加的洞天路标。

也可让安装器处理下载、校验、替换旧版本、添加任务和清理旧版预设路标：

```powershell
python tools/install_pack.py --game-dir 'D:/Minecraft/Forge-1.20.1'
python tools/install_pack.py --game-dir 'D:/Minecraft/server' --side server
```

运行安装器前关闭该游戏实例。旧版同名模组会保留为 `.before-pack.disabled`，被改动的配置和路标会留 `.bak` 备份；其他模组与存档内容保留。

## 洞天导航

- `J` 打开世界地图，`U` 打开路标列表，`N` 新建路标，`O` 打开 FTB 任务。原有 `M` 打坐和 `B` 突破保留；已有自定义地图按键不会覆盖。
- 不自动添加建筑路标。安装器移除之前添加的 8 个洞天预设点，保留玩家自己创建的路标。
- FTB 任务页“落霞洞天·山海行迹”列出八处目的地，走到对应入口附近会自动完成探索记录，不发放数值奖励。
- 世界地图随探索绘制。地图不会预先生成建筑或强制加载整个洞天。
- 只清理旧预设点时可运行 `python tools/install_pack.py --remove-pack-waypoints`；单人存档与多人服务器在新旧 Xaero 路标目录中的洞天预设点都会清理。

任务和坐标来源在 `pack/luoxia-landmarks.json`。当前任务坐标对应布局第二版；旧存档搬迁完成前，新址可能仍处于施工中。

## 下载与分发

`mods.lock.json` 固定版本、来源和 SHA-256；Modrinth 文件额外校验平台提供的 SHA-512。下载仅使用 FTB 官方 Maven 与作者在 Modrinth 发布的文件。仓库保存清单、配置和安装工具，第三方 JAR 保存在忽略的 `build` 目录中，公开分发时遵守各模组的许可。

## 已完成验证

2026-10-09：Forge 开发客户端加载全部模组并进入现有存档的副本。Xaero 建立 `xiuxian:luoxia_inner` 地图会话，读入 8 个预设路标；FTB 服务端和客户端各读入 8 个位置任务。FTB 前置全部加载时，14 项现有开局保护和洞天生成回归通过，项目构建成功。普通发行版启动器安装与真实多人服务器尚未实测。

随后按用户要求取消预设路标：安装器清理旧点且不再生成路标，地图与 8 个 FTB 探索任务保留。
