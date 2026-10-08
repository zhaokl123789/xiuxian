# 东方修仙方块扩展与建筑融合

2026-10-05：新增 **56 种**原创方块，与原有 24 种合计 **80 种**。新增部分包含 8 种纹饰建材与 48 种立体构件、灯具、修行和生活陈设；使用自制 JSON 模型与 32×32 纹理，不增加外部模组依赖。

## 实际建筑布置

落霞宗保留现有山体、建筑外形和黛瓦屋顶，更新白墙、朱柱、紫檀木、白玉边饰和云雕石基。屋檐使用落霞彩绘檐枋，屋脊、檐角增加凤翎脊与镇檐兽；不同高度的楼阁使用竹、梅、月纹花窗。侧殿分为经阁、丹剑修行、琴乐星仪等陈设，大殿使用编磬、灵牌、宗幡和传法玉座，庭园布置莲钵、仙鹤、石碑及庭灯。殿堂、塔楼与廊道采用宫灯、晶灯、香云悬灯与长明灯。

凡人城镇保留现有规模，六类店铺分别布置药柜/药架/丹炉、茶席/古琴/青花瓶、符案/经卷/笔墨、锦缎/屏风/盆景、米袋/酒坛/果筐、剑架/灵鼎/晶灯。民居增加竹影粉壁与花窗，院落增加水钵、庭灯和园景；宫殿增加礼乐与供奉陈设，屋顶加入彩绘檐枋、凤脊与镇檐兽。

按最终有序施工结果统计：落霞宗保留 48 种新增方块，城镇保留 45 种，两类并集覆盖全部 56 种。表中标记以最终保留方块为准，避免把会被后续施工覆盖的放置也计作融合。

## 新增方块目录

所有方块可在创造模式“建筑方块”页找到，也可使用 `/give @s xiuxian:<ID>`。

| 方块 | ID | 落霞宗 | 凡人城镇 | 发光等级 |
| --- | --- | --- | --- | --- |
| 霞纹灵玉石 | `xian_sunset_marble` | ✓ | — | 0 |
| 莲纹玉地砖 | `xian_lotus_floor` | ✓ | — | 0 |
| 星宿嵌金砖 | `xian_star_inlay` | ✓ | — | 0 |
| 阴阳太极砖 | `xian_taiji_tile` | ✓ | — | 0 |
| 浮雕流云石 | `xian_cloud_carved_stone` | ✓ | — | 0 |
| 蟠龙照壁砖 | `xian_dragon_relief` | ✓ | ✓ | 0 |
| 竹影粉壁 | `xian_bamboo_wall` | — | ✓ | 0 |
| 朱砂符纹砖 | `xian_cinnabar_rune_brick` | ✓ | — | 0 |
| 覆莲柱础 | `xian_lotus_column_base` | ✓ | — | 0 |
| 承云柱头 | `xian_jade_column_cap` | ✓ | — | 0 |
| 落霞彩绘檐枋 | `xian_sunset_eave` | ✓ | ✓ | 0 |
| 凤翎琉璃脊 | `xian_phoenix_ridge` | ✓ | ✓ | 0 |
| 青铜镇檐兽 | `xian_bronze_corner_beast` | ✓ | ✓ | 0 |
| 冰裂月纹花窗 | `xian_moon_window` | ✓ | ✓ | 0 |
| 翠竹花窗 | `xian_bamboo_window` | ✓ | ✓ | 0 |
| 梅花漏窗 | `xian_plum_window` | ✓ | ✓ | 0 |
| 嵌金云纹门扇 | `xian_carved_door_panel` | — | ✓ | 0 |
| 如意透雕楣 | `xian_cloud_transom` | ✓ | ✓ | 0 |
| 八宝宫灯 | `xian_palace_lantern` | ✓ | ✓ | 15 |
| 莲台灵灯 | `xian_lotus_lamp` | ✓ | ✓ | 15 |
| 青铜烛台 | `xian_bronze_candle` | ✓ | ✓ | 14 |
| 云纹壁灯 | `xian_wall_sconce` | — | ✓ | 15 |
| 灵晶镇灯 | `xian_spirit_crystal_lamp` | ✓ | ✓ | 15 |
| 香云悬灯 | `xian_incense_lamp` | ✓ | — | 15 |
| 竹影庭灯 | `xian_garden_lamp` | ✓ | ✓ | 15 |
| 宝塔长明灯 | `xian_pagoda_lamp` | ✓ | ✓ | 15 |
| 三足云纹香炉 | `xian_bronze_incense_burner` | ✓ | ✓ | 0 |
| 赤铜炼丹炉陈设 | `xian_alchemy_furnace` | ✓ | ✓ | 7 |
| 四象灵鼎陈设 | `xian_spirit_cauldron` | ✓ | ✓ | 0 |
| 莲纹蒲团 | `xian_meditation_cushion` | ✓ | ✓ | 0 |
| 玄木藏剑架 | `xian_sword_rack` | ✓ | ✓ | 0 |
| 朱砂制符案 | `xian_talisman_desk` | ✓ | ✓ | 0 |
| 玉轴经卷架 | `xian_scroll_stand` | ✓ | ✓ | 0 |
| 周天星仪 | `xian_astrolabe` | ✓ | — | 3 |
| 灵珠承露台 | `xian_jade_orb_pedestal` | ✓ | — | 9 |
| 金玉编磬 | `xian_chime_rack` | ✓ | ✓ | 0 |
| 落霞宗幡 | `xian_sect_banner` | ✓ | ✓ | 0 |
| 镇山悬钟 | `xian_ritual_bell` | ✓ | ✓ | 0 |
| 传法玉座 | `xian_scripture_pedestal` | ✓ | ✓ | 0 |
| 仙祖灵牌 | `xian_spirit_tablet` | ✓ | ✓ | 0 |
| 青瓷茶席 | `xian_tea_table` | ✓ | ✓ | 0 |
| 七弦古琴案 | `xian_qin_table` | ✓ | ✓ | 0 |
| 百草药斗柜 | `xian_apothecary_drawers` | ✓ | ✓ | 0 |
| 灵药晾架 | `xian_herb_drying_rack` | ✓ | ✓ | 0 |
| 青花长颈瓶 | `xian_porcelain_vase` | ✓ | ✓ | 0 |
| 封泥酒坛 | `xian_wine_jars` | — | ✓ | 0 |
| 灵谷麻袋 | `xian_rice_sacks` | — | ✓ | 0 |
| 灵果竹筐 | `xian_fruit_basket` | — | ✓ | 0 |
| 锦缎货架 | `xian_silk_display` | — | ✓ | 0 |
| 算盘账房柜 | `xian_abacus_counter` | — | ✓ | 0 |
| 笔墨砚台 | `xian_writing_set` | ✓ | ✓ | 0 |
| 湘竹隔扇 | `xian_bamboo_screen` | ✓ | ✓ | 0 |
| 青莲水钵 | `xian_lotus_basin` | ✓ | ✓ | 0 |
| 苍松盆景 | `xian_bonsai_pine` | ✓ | ✓ | 0 |
| 白玉仙鹤 | `xian_crane_statue` | ✓ | ✓ | 0 |
| 云篆石碑 | `xian_stone_stele` | ✓ | ✓ | 0 |

## 验收与旧存档

本轮扩展已接入两类建筑的新版生成流程，尚未接入洞天随机刷新。已完成的旧城镇不会自动重建；在同一测试存档的新位置使用下列道具或命令生成：

```mcfunction
/give @s xiuxian:mortal_town_inspection_token
/xiuxian town generate
/xiuxian town status
```

城镇清场涵盖东西 345、南北 257 格，原点上方至世界高度上限；地基替换原点下方 2 格至原点，深层地下保留。清场和施工继续按 tick 分批执行，避免一次性占用服务器线程。

落霞宗每存档只保留一处施工记录。验收完整新版外部，请在**新测试存档**使用营建令右键地面，再通过观察点命令查看：

```mcfunction
/give @s xiuxian:luoxia_construction_decree
/xiuxian luoxia visit entrance
/xiuxian luoxia visit court
/xiuxian luoxia visit summit
```

施工版本由 2 提高至 3：旧未完成任务不能按旧游标继续新版施工。城镇可先 `/xiuxian town cancel` 再选新位置；落霞宗的旧施工记录保持保留，完整新版使用新测试存档。已完成落霞宗不自动全面替换；若它仍有旧的追加陈设任务，重新从头扫描追加列表且只填空气，避免旧游标错位与覆盖现有方块。

当前丹炉、灵鼎、案桌、柜架等为装饰方块；未新增炼丹、储物、坐下功能或生存合成配方。主灯亮度 15，烛台 14，丹炉陈设 7，星仪 3，灵珠台 9。模型与碰撞同步支持四向放置。

## 模型预览与验证

预览读取实际模型 JSON 与纹理生成，**是离线模型图，不是游戏截图，也不模拟发光**：

- `build/oriental-preview/architecture.png`：18 种建材与建筑构件。
- `build/oriental-preview/cultivation.png`：22 种灯具与修行陈设。
- `build/oriental-preview/town-life.png`：16 种市井与园景陈设。
- `build/oriental-preview/building-usage.json`：两类建筑最终保留的每类方块数量。

```powershell
.\gradlew.bat build --no-daemon
.\gradlew.bat luoxiaVerify --no-daemon
.\gradlew.bat runGameTestServer '-PgameTestNamespaces=xiuxian_geometry,xiuxian_town' --no-daemon
```

`luoxiaVerify` 在 Forge 注册完成后运行几何测试，检查完整落霞宗蓝图、连续登山路径、门洞、廊桥、殿堂通道、屋顶体量与存档游标。联合测试包含 3 项：几何与新增方块最终存留、小范围清场/续建、跨冷区块的完整城镇施工；完整城镇同时核对药铺陈设、宫殿编磬和壁灯亮度。成功结果必须确认实际测试数量，不把 0 项测试视为通过。

构建产物为 `build/libs/xiuxian-1.0.0-SNAPSHOT.jar`。启动开发客户端只用于确认模型、纹理和注册资源能加载；建筑美观程度仍需玩家游戏内验收。

2026-10-05 验证结果：最终构建通过；联合 Forge GameTest 实际运行 3 项，全部通过。落霞宗几何统计为 8,250,465 个占用方块、107 种状态、14,175 项操作。客户端完成资源图集加载，未发现本模组模型、纹理加载错误；打包资源审计确认 80 种方块的模型、纹理、破坏粒子、物品、掉落表与双语名称齐全。日志分别位于 `build/expansion-build-final.log`、`build/expansion-gametest-final.log`、`build/expansion-client-final.log`。最终 JAR 资源审计可运行 `tools/audit_oriental_package.py`。
