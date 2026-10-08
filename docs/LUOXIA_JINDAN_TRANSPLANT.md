# 金丹居所移植到落霞洞天

复用已验收的金丹居所完整蓝图、136 种专用方块与六段主体施工，替换原来的小型占位宫殿。洞天内只生成一座。

- 原点：`(-360, 112, 220)`。
- 主体占地：X `-538..-182`，Z `56..384`，蓝图高度 Y `110..266`。
- 主体占地从 Y `113` 到世界高度上限全部清空、复扫，再建造。
- 旧金丹支路及灯柱按原通路体积清理；新阶梯从 `(0,72,160)` 接至东侧入口，再连接东殿。
- 施工分区块执行，游标保存在洞天自己的 `xiuxian_jindan_residences` 数据中，重载后继续。
- 当前洞天版本仍为 3，迁移只启动金丹居所施工。城镇、宗门、道胎宫、灵脉、Boss 区及其他地标保持原有数据。
- 已完工后再次进入不会重建，后续玩家改动保留。

创造模式且有管理员权限时可验收：

```mcfunction
/xiuxian jindan inner status
/xiuxian jindan inner visit entrance
/xiuxian jindan inner visit palace
/xiuxian jindan inner visit furnace
/xiuxian jindan inner visit view
```

传送要求施工完成。从洞天外使用时经过正常洞天入口逻辑保存返回点，用 `/xiuxian luoxia return` 返回。总览传送自动开启飞行。

验证：

```powershell
.\gradlew.bat runGameTestServer build --console=plain '-PgameTestNamespaces=xiuxian_geometry,xiuxian_jindan_inner,xiuxian_daotai_inner,xiuxian_clearance,xiuxian_startup,xiuxian_rebuild'
```

服务器测试覆盖完整蓝图落地、旧占位建筑及道路清理、边界、高处清场、其他地标保留、存档游标恢复、唯一性、通道及验收落点。真实客户端的远景、光照和跨维度网络传送需要游戏内验收。
