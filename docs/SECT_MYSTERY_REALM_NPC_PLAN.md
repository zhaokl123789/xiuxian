# 宗门、秘境、怪物与修士系统设计

本文档是下一阶段的实现契约，参考《玄鉴仙族》中“道途—宗门—资源地—人物关系”的组织方式，但所有名称、数值和掉落均使用本项目自己的数据。系统保持服务端权威，客户端只负责展示和发起请求。

## 设计边界

- **宗门**负责传承、兑换、任务和声望；功法与术法的获得渠道集中在宗门藏经阁、师承和宗门事件。
- **秘境**负责探索、战斗和奇遇；每个秘境有入口条件、规则、怪物池、首领和奖励池。
- **怪物**是可复用的战斗模板，境界、五行、抗性、弱点和技能由数据定义，AI 行为由少量策略类型驱动。
- **修士**是带有功法、道途、宗门身份和关系状态的 NPC，可交易、传授、切磋、发布任务或成为敌对目标。

## 核心数据模型

### 宗门 `SectDefinition`

`id`、`displayName`、`doctrine`、`regionId`、`requiredRealm`、`pathId`、`reputationTiers`、`contributionCurrency`、`techniqueIds`、`spellIds`、`taskPool`、`rankRules`、`relations`。

宗门职位按“杂役—外门—内门—真传—长老”分层。职位只改变可接任务、藏经阁权限和每日贡献上限，不直接替玩家跳过境界条件。道途限制通过 `pathId` 检查，通用宗门使用空值。

### 秘境 `MysteryRealmDefinition`

`id`、`displayName`、`entranceRule`、`recommendedRealm`、`openSchedule`、`cooldownTicks`、`qiMultiplier`、`areas`、`monsterPool`、`bossId`、`encounterPool`、`dropTable`、`failureRule`。

秘境运行时生成 `MysteryRealmRun`，记录种子、层数、已触发奇遇、首领状态和退出时间。功法、术法书、仙基材料只能从明确的掉落或奇遇渠道获得，禁止合成。

### 怪物 `MonsterArchetype`

`id`、`displayName`、`realm`、`element`、`maxHealth`、`attack`、`defense`、`resistances`、`weaknesses`、`spellIds`、`aiStyle`、`dropTable`、`elite`。

首批 AI 策略为“游荡、守门、伏击、护巢、首领阶段”。属性克制沿用功法与术法的五行关联，弱点只提供可观测的战斗反馈，不做隐藏秒杀。

### 修士 `CultivatorNpcDefinition`

`id`、`displayName`、`realm`、`techniqueId`、`pathId`、`sectId`、`rank`、`attributes`、`attitude`、`dialoguePool`、`questPool`、`tradeTable`、`teachTable`、`relationshipRules`。

NPC 的传授、兑换和切磋都由服务端校验境界、声望、前置功法和冷却。关系状态使用玩家 UUID 作为键，保存好感、宗门声望、任务阶段和敌对标记。

## 与功法、术法、仙基的联系

1. 宗门传承直接引用稳定的功法/术法 ID；修习功法后，功法专属术法立即进入客户端候选列表。
2. 术法书是独立掉落物，学习后写入 `learnedSpellIds`；专属术法的可用性仍由当前功法和境界共同决定。
3. 秘境奖励按道途、五行和宗门声望加权，不能通过合成绕过获取渠道。
4. 仙基材料在筑基及以上秘境开放，材料的道韵标签与功法 `qiAffinity` 做匹配，形成属性增幅或轻微减益。
5. 同一宗门的功法、术法和 NPC 共享 `resonanceGroup`，达到条件后提供可解释的属性联动，避免单纯堆数值。

## 分阶段实施

### 阶段 A：数据目录与只读展示

新增 `SectDefinitions`、`MysteryRealmDefinitions`、`MonsterArchetypes`、`CultivatorNpcDefinitions` 四个注册目录和对应 record。先提供中文名称、境界、道途、掉落和关联 ID，并在功法详情页显示“可从哪些宗门/秘境获得”。

### 阶段 B：宗门声望与藏经阁

增加服务端声望 capability、贡献点和藏经阁兑换请求。所有兑换先验证境界、前置功法、道途和库存，再同步结果；客户端提供宗门面板和兑换记录。

### 阶段 C：秘境运行时

实现入口检查、实例种子、区域推进、怪物池、奇遇和退出结算。首批只做一处胎息秘境，验证掉落、死亡风险和重复进入冷却。

### 阶段 D：怪物与修士

先接入三种怪物策略和三名修士 NPC，再扩展任务链、交易、传授、切磋和敌对关系。NPC 的战斗能力复用术法目录和功法属性，不复制一套平行战斗公式。

## 验收标准

- 切换功法后无需重启或重开界面，装配页能看到对应专属术法；服务端拒绝不匹配的装备请求。
- 宗门兑换、秘境掉落、术法书学习、NPC 传授均能在存档重载后保持。
- 玩家能从界面看到境界门槛、道途限制、获取渠道、掉落来源和失败风险。
- 所有玩家可见文本使用中文，稳定 ID 只用于代码和存档。
- 新系统异常时不重置境界、血量上限、真气或身份状态。

