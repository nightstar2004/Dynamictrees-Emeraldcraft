# DynamicTrees Emeraldcraft

为 [EmeraldCraft](https://github.com/Viola-Siemens/EmeraldCraftMod) 的四种原生树添加  
[DynamicTrees](https://github.com/DynamicTreesTeam/DynamicTrees) 动态树支持的附属模组。

- Minecraft 1.20.1 / Forge 47.x
- 纯数据驱动实现（`trees/<modid>/` JSON），Java 侧仅有主类与少量常量

## 支持的树种

| 树种  | 英文          | EC 方块                                     | 动态树类型                        |
| --- | ----------- | ----------------------------------------- | ---------------------------- |
| 银杏  | Ginkgo      | `ginkgo_log` / `ginkgo_sapling`           | 常规乔木                         |
| 棕榈  | Palm        | `palm_log` / `palm_sapling`               | `dynamictrees:palm`          |
| 桃树  | Peach       | `peach_log` / `peach_sapling`             | 常规乔木                         |
| 紫菌木 | Purpuraceus | `purpuraceus_stem` / `purpuraceus_fungus` | `dynamictrees:nether_fungus` |

## 特性

- 四种树在对应 EC 生物群系（ginkgo_forest / palm_beach / xanadu / purpuraceus_swamp）中  
  替换原版静态树生成（`world_gen/default.json` + `feature_cancellers.json` 数据驱动）
- 动态树干、树叶、树苗模型，棕榈叶 multipart 渲染
- 原创树皮 / 年轮 / 菌盖 / 种子纹理（程序化生成，不复制 EC 资产）
- 战利品表（branches / leaves / voluntary）

## 依赖

| 依赖                                                                        | 版本             | 要求      |
| ------------------------------------------------------------------------- | -------------- | ------- |
| Minecraft Forge                                                           | 1.20.1 / 47.x  | 编译 + 运行 |
| [DynamicTrees](https://www.curseforge.com/minecraft/mc-mods/dynamictrees) | 1.20.1（1.4.11） | 编译 + 运行 |
| EmeraldCraft                                                              | 1.20.1（9.1.X）  | 仅运行     |

## 构建

本项目编译依赖通过本地 `libs/` 目录（flatDir）解析，仓库**不包含**第三方 jar，  
克隆后需先自行准备依赖：

1. 下载 DynamicTrees 1.20.1 **1.4.11** 版本（CurseForge），放入  
   `libs/DynamicTrees-1.20.1-1.4.11.jar`。
2. 构建：
   ```bash
   ./gradlew build
   ```
   产物位于 `build/libs/dynamictrees_emeraldcraft-0.1.jar`。

> EmeraldCraft 本体**无需**放入 `libs/`（本项目仅以字符串引用其方块 ID，  
> 不直接依赖其类），但运行时需将其放入游戏 `mods/` 目录。

## 许可

- 代码：ISC License，见 [LICENSE](LICENSE)。
- 资产（纹理 / 模型 / 语言文件）：本项目原创，随 ISC License 一并发布。

本模组不包含、不复制 EmeraldCraft 的任何代码或资产  
（EmeraldCraft 源码为 MIT 许可，但其资产为 All Rights Reserved）。
