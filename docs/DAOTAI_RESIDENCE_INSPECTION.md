# 道胎云宫外部验收

依据 `G:/mc/references/luoxia/04.png` 构建。当前只在主世界通过验收令或命令生成，待玩家验收后再决定移植到落霞洞天。

## 建筑

- 预留范围：东西 441 格、南北 421 格；相对原点高度 -2 到 222，共 225 格。占地约为落霞外部预留范围的 1.5 倍。
- 三重白玉环台的主体高度为 28、48、66；道宫平台为 74；上层殿阁为 113、143。
- 深青屋面、金色脊饰与翘角、玉柱、发光金轮、浮空石岛、亭阁、宝塔、灵池及玻璃围护的水景。
- 殿内有专属家具、经架、符案、琴台、香炉与蒲团；地板嵌灯与专属灯具共同照明。
- 上层使用主殿外侧的楼梯与廊桥进入。施工先完整清场并复扫，再分区块建造，保存进度，范围内阻止普通怪物自然生成。

## 生成

创造模式且具有管理员权限时：

```mcfunction
/give @s xiuxian:daotai_residence_inspection_token
/xiuxian daotai generate
/xiuxian daotai generate <x> <y> <z>
/xiuxian daotai status
/xiuxian daotai cancel
/xiuxian daotai rebuild
```

验收令右键地面使用，以点击方块作为原点。普通主世界原点须在 Y=97 或更低，建议 Y=64，以容纳 222 格高的上部建筑。整个 441×421 占地内，从原点上一格清到世界高度上限，包含建筑间隙、顶部、箱子和库存。范围外及更深地下保留。

生成完成后，可直接到最近一座已完成云宫的验收点：

```mcfunction
/xiuxian daotai visit entrance
/xiuxian daotai visit palace
/xiuxian daotai visit upper
/xiuxian daotai visit view
```

`view` 为高空远景点，会开启创造飞行。旧版已完成建筑不会自动重建；请选另一处空地生成新版。旧版尚未完成的施工会因版本不兼容暂停，可先取消再重新生成。

## 验证

```powershell
.\gradlew.bat build --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain '-PgameTestNamespaces=xiuxian_geometry,xiuxian_daotai'
```

测试检查最终方块的主轴和高层通路、装饰保留量、规模与浮空留白，并在独立测试世界完整施工。诊断图输出到 `build/daotai-preview/`，颜色和模型已简化，并非游戏截图。游戏里的远景、夜景和材质观感仍需玩家验收。
