"""Keep the expansion inventory tied to the authored block definitions."""
import json
from pathlib import Path
from oriental_expansion import DEFINITIONS

root = Path(__file__).resolve().parents[1]
usage = json.loads((root / 'build/oriental-preview/building-usage.json').read_text(encoding='utf-8'))
sect = usage['luoxiaSurvivingBlocks']
town = usage['townSurvivingBlocks']
intro = '''# 东方修仙方块扩展与建筑融合

2026-10-05：新增 **56 种**原创方块，与原有 24 种合计 **80 种**。新增部分包含 8 种纹饰建材与 48 种立体构件、灯具、修行和生活陈设；使用自制 JSON 模型与 32×32 纹理，不增加外部模组依赖。

## 实际建筑布置

落霞宗保留现有山体、建筑外形和黛瓦屋顶，更新白墙、朱柱、紫檀木、白玉边饰和云雕石基。屋檐使用落霞彩绘檐枋，屋脊、檐角增加凤翎脊与镇檐兽；不同高度的楼阁使用竹、梅、月纹花窗。侧殿分为经阁、丹剑修行、琴乐星仪等陈设，大殿使用编磬、灵牌、宗幡和传法玉座，庭园布置莲钵、仙鹤、石碑及庭灯。殿堂、塔楼与廊道采用宫灯、晶灯、香云悬灯与长明灯。

凡人城镇保留现有规模，六类店铺分别布置药柜/药架/丹炉、茶席/古琴/青花瓶、符案/经卷/笔墨、锦缎/屏风/盆景、米袋/酒坛/果筐、剑架/灵鼎/晶灯。民居增加竹影粉壁与花窗，院落增加水钵、庭灯和园景；宫殿增加礼乐与供奉陈设，屋顶加入彩绘檐枋、凤脊与镇檐兽。

按最终有序施工结果统计：落霞宗保留 48 种新增方块，城镇保留 45 种，两类并集覆盖全部 56 种。表中标记以最终保留方块为准，避免把会被后续施工覆盖的放置也计作融合。

## 新增方块目录

所有方块可在创造模式“建筑方块”页找到，也可使用 `/give @s xiuxian:<ID>`。

| 方块 | ID | 落霞宗 | 凡人城镇 | 发光等级 |
| --- | --- | --- | --- | --- |
'''
rows = []
for block_id, (zh, en, material, light, geometry) in DEFINITIONS.items():
    rows.append(f"| {zh} | `{block_id}` | {'✓' if sect.get(block_id) else '—'} | {'✓' if town.get(block_id) else '—'} | {light} |")
outro = '''

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
.\\gradlew.bat build --no-daemon
.\\gradlew.bat luoxiaVerify --no-daemon
.\\gradlew.bat runGameTestServer '-PgameTestNamespaces=xiuxian_geometry,xiuxian_town' --no-daemon
```

`luoxiaVerify` 在 Forge 注册完成后运行几何测试，检查完整落霞宗蓝图、连续登山路径、门洞、廊桥、殿堂通道、屋顶体量与存档游标。联合测试包含 3 项：几何与新增方块最终存留、小范围清场/续建、跨冷区块的完整城镇施工；完整城镇同时核对药铺陈设、宫殿编磬和壁灯亮度。成功结果必须确认实际测试数量，不把 0 项测试视为通过。

构建产物为 `build/libs/xiuxian-1.0.0-SNAPSHOT.jar`。启动开发客户端只用于确认模型、纹理和注册资源能加载；建筑美观程度仍需玩家游戏内验收。
'''
(root / 'docs/ORIENTAL_BLOCK_EXPANSION.md').write_text(intro + '\n'.join(rows) + outro, encoding='utf-8')

p = root / 'docs/MORTAL_TOWN_INSPECTION.md'
s = p.read_text(encoding='utf-8')
s = s.replace('共 24 种', '共 80 种（原有 24 种，本轮新增 56 种；完整清单见 [东方修仙方块扩展](ORIENTAL_BLOCK_EXPANSION.md)）')
s = s.replace('`xiuxian:town_*` ID', '`xiuxian:town_*` 或 `xiuxian:xian_*` ID')
s = s.replace('-PgameTestNamespaces=xiuxian_town', "'-PgameTestNamespaces=xiuxian_town'")
s += '\n新增六类店铺陈设、殿堂礼器、民居花窗、屋脊檐角和庭园灯饰已接入新版生成。新增模型预览：`build/oriental-preview/architecture.png`、`cultivation.png`、`town-life.png`。旧版未完成任务因施工顺序改变而暂停，请取消后重新选址；已完成城镇不自动覆盖。\n'
p.write_text(s, encoding='utf-8')
p = root / 'docs/LUOXIA_EXTERIOR_CONCEPT.md'
s = p.read_text(encoding='utf-8')
s = s.replace('独立 JVM 不测试 Forge 网络钩子或服务器事件。', '该检查现在通过 Forge GameTest 启动，以保证自制方块注册完成；它只检查蓝图与状态，不等同于完整服务器施工测试。')
s += '\n2026-10-05：保留原有山体、黛瓦屋顶与外形，融合自制修仙建材、花窗、檐脊构件、礼器、灯具和陈设，详见 [东方修仙方块扩展](ORIENTAL_BLOCK_EXPANSION.md)。施工版本提高至 3，旧未完工游标不再自动升级；验收完整新版请使用新测试存档。\n'
p.write_text(s, encoding='utf-8')
print('Wrote expansion inventory and updated both building guides.')
