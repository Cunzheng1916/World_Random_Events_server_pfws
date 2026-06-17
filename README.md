# World Random Events (世界随机事件)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.5-green?style=flat-square)](https://minecraft.net)
[![Fabric API](https://img.shields.io/badge/Fabric_API-0.145.4%2B26.1.1-blue?style=flat-square)](https://fabricmc.net)
[![Loader](https://img.shields.io/badge/Fabric_Loader-0.19.3-orange?style=flat-square)](https://fabricmc.net)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

一个为 Minecraft Fabric 服务端设计的**纯服务端**模组，在主世界中随机触发各种世界事件——包含灾难、祝福和中立事件，为生存服务器增添动态挑战与乐趣。

> A **server-only** Fabric mod that randomly triggers world events in the Overworld — featuring disasters, blessings, and neutral events — adding dynamic challenges and fun to survival servers.

---

## 📋 目录

- [事件列表](#-事件列表)
- [命令系统](#-命令系统)
- [玩家交互](#-玩家交互)
- [配置文件](#-配置文件)
- [安装方法](#-安装方法)
- [事件触发机制](#-事件触发机制)
- [开发构建](#-开发构建)

---

## 🌍 事件列表

### 🔴 灾难事件 (Disasters)

| 事件 | ID | 触发间隔 | 触发概率 | 持续时间 | 冷却 | 强制触发 |
|------|-----|---------|---------|---------|------|---------|
| **血月** Blood Moon | `blood_moon` | 31 天 | 100% | 1 夜 (~13000t) | 31 天 | 31 天 |
| **猪灵入侵** Piglin Invasion | `piglin_invasion` | 3 天 | 30% | 12 天 | 3 天 | 45 天 |
| **天降咸鱼** Fish Rain | `fish_rain` | 5 天 | 25% | 2 分钟 | 5 天 | 30 天 |

#### 🩸 血月 (Blood Moon)
- 天空变为血红色
- 所有怪物获得 **力量 III**、**速度 II**、**抗性 I**
- 是挑战怪物刷怪塔和高难度生存的终极考验

#### 🐷 猪灵入侵 (Piglin Invasion)
- 在玩家附近生成 2 个入侵编队（僵尸猪灵、僵尸疣猪兽、烈焰人）
- 玩家周期性获得 **凋零** 和 **虚弱** 效果
- 事件结束后玩家残留虚弱效果 2 天

#### � 天降咸鱼 (Fish Rain)
- 天空变为深蓝色
- 每秒在玩家周围 **50 格范围**内随机掉落 **3~5 条**鱼类物品
- 掉落鱼种：生鳕鱼、生鲑鱼、热带鱼、河豚、熟鳕鱼、熟鲑鱼

---

### 🟢 祝福事件 (Blessings)

| 事件 | ID | 触发间隔 | 触发概率 | 持续时间 | 冷却 |
|------|-----|---------|---------|---------|------|
| **流星雨** Meteor Shower | `meteor_shower` | 5 天 | 20% | 4200t | 5 天 |
| **丰收祭典** Harvest Festival | `harvest_festival` | 12 天 | 35% | 1 天 | 12 天 |

#### ☄️ 流星雨 (Meteor Shower)
- 全体玩家获得 **幸运 I**
- 在玩家周围生成陨石坑（岩浆块 + 破坏地面）
- 陨石掉落物概率：
  - 1% → **钻石** 或 **下界合金碎片**
  - 9% → 金锭（1-2 个）
  - 90% → 铁粒/铜锭/煤炭/经验瓶
- 不会破坏箱子、床、工作台、附魔台等重要方块

#### 🌾 丰收祭典 (Harvest Festival)
- 全体玩家获得 **幸运 II** + **生命恢复 I**
- 生成丰收祭坛和丰收精灵
- 每 **30 秒**自动收割玩家周围 32 格内成熟作物并重新种植
- 每 **10 秒**刷新玩家生命恢复效果
- 每 **10 秒**对周围农作物随机使用 **1~3 次骨粉**加速生长
- 事件结束时，根据玩家附近作物数量奖励经验（每株 5 点）

---

### 🟡 中立事件 (Neutral)

| 事件 | ID | 触发间隔 | 触发概率 | 持续时间 | 冷却 | 强制触发 |
|------|-----|---------|---------|---------|------|---------|
| **远征商队** Caravan Expedition | `caravan_expedition` | 6 天 | 40% | 2~5 天 | 6 天 | 60 天 |

#### 🐪 远征商队 (Caravan Expedition)
- 生成 5~7 个「远征商人」（发光流浪商人 + 缓慢 II + 生命恢复 II）
- 2~4 个近战守卫（铁傀儡，速度 III + 力量 V + 400HP）
- 4~6 个远程守卫（雪傀儡，力量 VI）
- 商队会保持在生成位置附近活动

---

## 🎮 命令系统

所有命令需要 **游戏管理员权限**（2 级），根命令为 `/worldevents`：

| 命令 | 说明 |
|------|------|
| `/worldevents list` | 列出全部事件及状态（就绪/运行中/冷却中） |
| `/worldevents status` | 查看当前正在运行的事件 |
| `/worldevents start <id>` | 强制启动指定事件（跳过冷却和条件检查） |
| `/worldevents stop` | 强制结束当前事件 |
| `/worldevents reset <id>` | 重置指定事件的冷却时间 |
| `/worldevents reload` | 重新加载配置文件 |

事件 ID 参考：`blood_moon`, `piglin_invasion`, `fish_rain`, `meteor_shower`, `harvest_festival`, `caravan_expedition`

---

## 🖐 玩家交互

当前版本暂无特殊玩家交互事件。

---

## ⚙️ 配置文件

配置文件位于：`config/world-random-events/config.json`

首次启动时自动生成默认配置，支持热重载（`/worldevents reload`）。

```json
{
  "global_enabled": true,
  "structure_mode": "BUILTIN",
  "events": {
    "blood_moon":         { "enabled": true, "type": "DISASTER" },
    "piglin_invasion":    { "enabled": true, "type": "DISASTER" },
    "fish_rain":          { "enabled": true, "type": "DISASTER" },
    "meteor_shower":      { "enabled": true, "type": "BLESSING" },
    "harvest_festival":   { "enabled": true, "type": "BLESSING" },
    "caravan_expedition": { "enabled": true, "type": "NEUTRAL" }
  }
}
```

| 设置项 | 说明 |
|--------|------|
| `global_enabled` | 全局开关，`false` 禁用所有事件 |
| `structure_mode` | 结构生成模式（`BUILTIN` 内置 / `STRUCTURE` 外部结构文件） |
| `events.<id>.enabled` | 单独禁用/启用某个事件 |

---

## 📦 安装方法

### 服务端安装

1. 确保服务端运行 **Minecraft 1.21.5** 的 **Fabric** 服务端
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api) (0.145.4 或更高版本)
3. 将 `world-random-events-1.0.0.jar` 放入 `mods/` 文件夹
4. 启动服务端

> ⚠️ **仅限服务端！** 本模组是纯服务端模组，客户端无需安装。

### 环境要求

| 组件 | 版本 |
|------|------|
| Minecraft | 1.21.5 (26.1.1) |
| Fabric Loader | 0.19.3+ |
| Fabric API | 0.145.4+26.1.1+ |
| Java | JDK 25 |

---

## 🎲 事件触发机制

1. **每日检查**：每个游戏日结束时（24000t），检查是否触发事件
2. **触发条件**：
   - 主世界必须有至少一个玩家
   - 该玩家不在 Y≤-32 的深层地下
   - 玩家 32 格内没有监守者 (Warden)
   - 没有正在运行的事件
3. **优先级**：灾难 > 中立 > 祝福 —— 每种类型最多触发一个
4. **概率计算**：先检查是否达到强制触发天数，否则按概率随机
5. **冷却机制**：事件结束后进入冷却，冷却期间不会再次触发
6. **强制触发**：部分事件有保底天数，超过后必定触发

```
每个游戏日
  ├─ 有玩家在线？
  ├─ 玩家不在深层地下？
  ├─ 玩家附近无监守者？
  ├─ 当前无事件运行？
  └─ 按优先级检查
       ├─ DISASTER (灾难) → 选一个
       ├─ NEUTRAL  (中立) → 选一个
       └─ BLESSING (祝福) → 选一个
```

---

## 🔧 开发构建

```bash
# 克隆仓库
git clone https://github.com/Cunzheng1916/World_Random_Events_server_pfws.git
cd World_Random_Events_server_pfws
git checkout Dev

# 构建
./gradlew build

# 生成的 JAR 位于 build/libs/world-random-events-1.0.0.jar
```

### 技术栈

| 组件 | 版本 |
|------|------|
| Gradle | 9.2.0 |
| Fabric Loom | 1.15.5 |
| JDK | 25 |

---

## 📁 项目结构

```
src/main/java/com/pfws/worldrandomevents/
├── WorldRandomEvents.java          # 模组入口
├── config/
│   └── ModConfig.java              # 配置管理
├── command/
│   └── EventCommands.java          # 命令注册
├── event/
│   ├── BaseEvent.java              # 事件基类
│   ├── EventManager.java           # 事件调度管理
│   ├── EventInteractionHandler.java # 玩家交互处理
│   ├── disaster/
│   │   ├── BloodMoon.java          # 血月
│   │   ├── PiglinInvasion.java     # 猪灵入侵
│   │   └── FishRain.java           # 天降咸鱼
│   ├── blessing/
│   │   ├── MeteorShower.java       # 流星雨
│   │   └── HarvestFestival.java    # 丰收祭典
│   └── neutral/
│       └── CaravanExpedition.java  # 远征商队
├── mixin/
│   ├── CaravanGuardAiMixin.java    # 商队守卫AI控制
│   ├── LivingEntityMixin.java      # 生物死亡掉落增强
│   ├── MobMixin.java               # 怪物掉落率调整
│   ├── ServerLevelMixin.java       # 实体生成时强化
│   ├── SleepStatusMixin.java       # 血月禁止睡眠
│   └── SnowballMixin.java          # 商队雪球防误伤
└── network/
    └── NetworkHandler.java         # 网络数据包
```

---

## 📝 更新日志

### v1.0.3 (2026-06-15)

**远征商队 数值对齐 + 掉落修复：**
- 🐛 **守卫掉落物修复**：修复近战守卫被击杀后不掉落物品的 BUG — `LivingEntityMixin.onDropAllDeathLoot()` 改为通过 `CaravanExpedition.ACTIVE_INSTANCE` 静态引用判定事件状态，确保在事件结束清理过程中守卫也能正常掉落
- ⚖️ **守卫属性对齐设计文档**：近战守卫 HP 400→450，远程守卫 HP 180→240
- ⚖️ **交易系统全面重写**：严格按照设计文档 `世界事件.txt` 重新实现
  - 每个商人上限 11 个交易（原 8~13）
  - 附魔钻石装备：1~3 个交易，1~2 种类型（原 4 种类型概率过高）
  - 附魔铁装备：4~6 个交易，1~3 种类型（原 2 种类型偏少）
  - 下界合金装备：5% 概率，无附魔，需下界之星 + 钻石块兑换
  - 杂物：种子/树苗/史莱姆块/蜂蜜块/紫水晶碎片/钻石块 等，最多 8 种

### v1.0.2 (2026-06-15)

**远征商队 全面修复：**
- 🐛 **事件结束逻辑重写**：移除商人死亡提前结束事件的逻辑；事件仅由自然计时结束或管理员 `/worldevents stop` 指令结束；`onEnd()` 改为全维度 `getAllEntities()` 扫描，确保所有远征商人/守卫被彻底清空（解决守卫追杀玩家跑远后局部扫描遗漏的问题）
- 🐛 **生物生成在地底**：`findSurface()` 和 `isSpawnSafe()` 增加 `canSeeSky()` 天空检测，确保生物只生成在露天位置；搜索尝试次数从 15 提升到 30
- 🐛 **守卫仇恨逻辑优化**：改为血量变化触发 — 商人受伤时自动寻找 60 格内最近玩家建立永久仇恨，替代旧的 `getLastHurtByMob` + `hurtTime` 逻辑
- 🐛 **远程守卫误伤近战守卫**：`SnowballMixin` 增加友方检测，雪球击中商队守卫/商人时跳过伤害

### v1.0.1 (2026-06-15)

**Bug 修复：**
- 🐛 **BaseEvent**: 修复永久持续事件（如神秘方尖碑）在首 tick 即被误结束的严重 bug — 增加 `totalDurationTicks > 0` 守卫条件
- 🐛 **神秘方尖碑**: 修复方尖碑立刻崩为掉落物的 bug（由上述 BaseEvent 修复解决）；新增地表查找失败时的安全退出机制
- 🐛 **猪灵入侵**: 修复猪灵全部死亡后事件不结束的 bug（`isAllDead()` 逻辑漏洞）；修复 `onEnd()` 中虚弱效果无法被移除的 bug；修复护甲掉落率设为 0% 导致不掉奖励的 bug
- 🐛 **猪灵入侵**: 新增入侵生物死亡掉落（金块 3~8、钻石 2~5、经验瓶 10~20、8% 远古残骸）
- 🐛 **远征商队**: 修复商人全部死亡后守卫实体泄漏且事件不结束的 bug

**功能完善：**
- ✨ 所有有坐标的事件（猪灵入侵、丰收祭典、远征商队）现在均会在开始/结束时广播事件坐标
- ✨ 远征商队守卫死亡时掉落丰厚奖励（钻石、铁块、经验瓶、远古残骸）

### v1.0.5 (2026-06-18)

**事件调整：**
- 🔥 **移除事件**：删除灵魂风暴、地脉紊乱、流浪铁匠、神秘方尖碑、坤坤入侵（共 5 个事件）
- 🆕 **新增事件**：天降咸鱼 — 每秒钟在玩家周围随机掉落 3~5 条鱼类物品
- 🌾 **丰收祭典增强**：新增骨粉加速农作物生长效果（每 10 秒对周围作物使用 1~3 次骨粉）
- 📦 **移除资源包**：删除坤坤资源包（音效/纹理），不再需要客户端资源包

---

## 📄 许可

MIT License

---

Made with ❤️ for the Minecraft Fabric community.
