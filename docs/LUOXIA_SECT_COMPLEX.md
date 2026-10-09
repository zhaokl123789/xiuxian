# 落霞完整宗门建筑群：独立验收版

本版承接 144 种宗门专属资源及 `G:/mc/references/luoxia/02.png`。建筑群先在主世界独立建造、验收，尚未替换落霞洞天的旧宗门，也未增加随机宗门站点。

## 规模与布局

- 总场地：**449×513**，X `-224..224`、Z `-256..256`，约为落霞外部建筑 `321×385` 占地的 1.86 倍。
- 蓝图高度：相对基准 Y `-24..154`；主世界基准 Y 有效范围 `-40..165`，建议 `40`。
- 主轴地面逐级由 Y `4` 抬升至 `12 / 24 / 46 / 70 / 84`，有连续台阶和宽阔平台。
- **31 栋建筑**：山门、双重檐主殿、讲经堂、执事院、茶堂、音律堂、丹院、器院、四层藏经楼、三层钟楼与鼓楼、12 栋弟子房、4 栋长老院堂、4 座园林与观景亭。
- 配套：左右演武阵台、药园、临水庭院、八组廊桥连接、外围山崖、两处瀑布、庭院树木与仪式纹饰。

最终几何检查统计：约 **882 万方块**，实际使用 **131 种专属资源**，含 **992 盏灯、196 个可入座陈设、25 处乐器**。这是按最终状态统计，包含重复放置同种资源，并非把每次放置算作一种资源。

## 可用交互

- 椅、长凳、蒲团可以入座，潜行离座。
- 音律堂、钟鼓楼及观景亭的乐器可以播放/停止短曲，潜行右键切换曲调。
- 灯具可以开关，演奏与灯具沿用专属资源阶段的存档规则。
- 弟子房有正常的床与储物箱。
- 丹院使用已有灵品炼丹炉；器院有原版熔炉、铁砧及工作台。
- 建筑有实际入口、穿堂通路和上楼台阶；门饰放在通路旁，仍属于静态装饰。
- 建筑范围禁止普通敌对生物自然生成，保留命令召唤等非自然生成。

NPC、宗门任务、执事交易、护山阵战斗和随机站点生成不属于当前建筑验收版。

## 建造与验收

创造模式管理员取得验收令：

```mcfunction
/give @s xiuxian:sect_complex_inspection_token
```

右键场地基准方块，或使用明确坐标：

```mcfunction
/xiuxian sect generate 12000 40 12000
/xiuxian sect status
/xiuxian sect visit view
/xiuxian sect visit entrance
/xiuxian sect visit main
/xiuxian sect visit library
/xiuxian sect visit music
/xiuxian sect visit garden
/xiuxian sect visit water
```

`generate` 会从**基准 Y-24 到世界顶端**完整清空 449×513 场地，包括未放置建筑的空隙与旧容器。选址须远离已有建筑。建造启动后，操作者会开启飞行并移至总览点。

施工按 `CLEAR → CLEAR_VERIFY → BUILD` 分阶段执行，每次限一个区块、4096 次方块修改及约 8ms 工作预算。区块生成等待不计入这个修改循环预算。施工游标写入世界存档，退出重进后继续。

- `/xiuxian sect cancel`：取消施工，保留已写入方块。
- `/xiuxian sect retry`：对暂停任务从完整清场重新开始。
- `/xiuxian sect rebuild`：重建最后一处已完成宗门，再次执行完整清场。
- 完工后才开放各验收传送点；同一基准位置不会自动重复施工。

## 验证与预览

`SectComplexVerification` 按最终方块状态检查主轴、院落通路、各楼层穿堂、楼梯净空、验收落点和资源保留量。

`SectComplexGameTests` 验证真实未加载区块中的完整施工、最高处及边界清场、旧容器清理、存档恢复、场地外方块保留、完工防重复、建成后的座位和乐器、传送命令以及自然刷怪边界。

最终构建和实际 Forge GameTest 的 24 项测试全部通过，包括原有开局身份与血量保护回归。

几何预览位于 `build/sect-complex-preview/`：

- `sect-isometric.png`：整体斜视。
- `sect-top.png`：俯视布局。
- `sect-axis.png`：中轴剖面。
- `blueprint.json`：按顺序执行的完整蓝图与建筑、传送点信息。

预览使用简化方块外形和颜色，**不是 Minecraft 游戏截图**。实际客户端外观、音乐听感与多人表现仍需进游戏验收。
# Cave-heaven transplant

The accepted 449 x 513 complex now builds once in Luoxia's inner dimension at
origin `(390, 68, -1800)`. The existing realm version stays at 3: old saves keep
their seed, city, vein and boss arena. The palaces relocate to distant sites.
New realms generate the complete complex during world loading, before entry.
Existing interrupted initial builds at the current coordinates are completed on
load. Older layouts schedule the independent transplant and saved jobs resume.

The new site clears from origin Y-24 to world ceiling. The retired central sect
clears its entire reserved site (X -58..58, Z 47..153, Y 65..world ceiling),
including gaps and containers, before restoring the avenue. The former mountain
site at `(480,68,-520)` clears independently and returns to natural ground.
The connecting causeway runs north from the town gate at `(390,64,101)`
to the accepted entrance; its walking volume is cleared, lit and railed.
The accepted interiors, usable seats and instruments remain in the blueprint.
Completed sites retain player edits on subsequent entries.

See [the current realm layout](LUOXIA_LAYOUT_V2.md) for all landmark coordinates
and the one-time migration coverage.

```mcfunction
/xiuxian sect inner status
/xiuxian sect inner retry
/xiuxian sect inner visit entrance
/xiuxian sect inner visit main
/xiuxian sect inner visit library
/xiuxian sect inner visit music
/xiuxian sect inner visit garden
/xiuxian sect inner visit water
/xiuxian sect inner visit view
```

Inspection visits require creative mode and permission level 2, and remain
unavailable until construction finishes. Cross-dimension visits use the existing
Luoxia entry system so the return anchor is recorded normally.

`LuoxiaSectComplexGameTests` runs the complete transplant in the actual inner
dimension, checks clearance/build cursor reloads, all authored blueprint sample
positions, continuous approaches, safe inspection destinations, preserved adjacent
regions and unique migration. Client-side visual acceptance remains a game check.
