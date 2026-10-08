# 道胎云宫移植至落霞洞天

验收通过的 `DaotaiResidenceGenerator` 第 2 版蓝图完整复用，保留 441 × 421 的建筑占地、三重云台、浮岛、宫殿、金色法环、家具和照明。洞天内只有一处固定的道胎居所，不参与普通城镇的随机刷新。

## 位置与通行

- 建筑原点：洞天 `(0, 80, -500)`。
- 占地：X `-220..220`，Z `-710..-290`，高度 Y `78..302`，适配现有维度高度。
- 入口：洞天北侧、云桥终点，约 `(0, 82, -291)`。
- 主殿：`(0, 155, -575)`；上层：`(0, 224, -592)`。
- 从洞天到达台向北走，穿过新开的通道，沿白玉云桥进入宫门；桥上有护栏、照明和缓坡石阶。宫内沿用已验收的楼梯与通路。

创意管理员可使用：

```mcfunction
/xiuxian daotai inner status
/xiuxian daotai inner visit entrance
/xiuxian daotai inner visit palace
/xiuxian daotai inner visit upper
/xiuxian daotai inner visit view
```

`visit` 要求施工完成，`view` 会开启创意飞行。从外部使用 `inner visit` 时走现有洞天进入流程，保存原来的返回锚点，可用 `/xiuxian luoxia return` 返回。

## 旧存档与施工

维度加载或进入时自动排队施工。洞天整体版本仍为 3；凡人城镇、宗门、金丹居所、灵脉、Boss 预留区域不因这次移植而重建。

原道胎小建筑的 X `-70..70`、Y `226..310`、Z `340..520` 区域会清空，旧天梯自第 3 阶起逐段移除，保留低处交汇道路。新宫清场限于自身占地和上方净空，云桥仅清理通行范围。

每 tick 施工扫描最多 32768 格、改写最多 3072 格，循环时间预算 8 ms。区块按需加载，进度和完成位置保存于维度自己的 `xiuxian_daotai_residences` SavedData；退出后再进入会恢复施工。完工后不会因重复进入而再次生成，也不会覆盖玩家后续改建。

外部验收道具和原来的 `/xiuxian daotai generate/status/visit` 命令继续用于主世界独立验收建筑。

## 验证

```powershell
.\gradlew.bat runGameTestServer build --no-daemon --console=plain '-PgameTestNamespaces=xiuxian_geometry,xiuxian_daotai_inner,xiuxian_daotai'
python tools/audit_oriental_package.py
```

洞天测试覆盖旧版迁移、完整冷区块施工、NBT 进度恢复、唯一性、清场边界、地标高度、家具照明、从入口到主殿逐格通行，以及其他区域不受清场影响。

本次验证结果：4 项 GameTest 全部通过；模组构建通过；212 种东方方块的资源链检查通过，其中包括 132 种道胎专属方块。
