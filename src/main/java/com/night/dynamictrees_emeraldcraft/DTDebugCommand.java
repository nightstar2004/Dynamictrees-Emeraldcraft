package com.night.dynamictrees_emeraldcraft;

import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.worldgen.BiomeDatabase;
import com.ferreusveritas.dynamictrees.worldgen.BiomeDatabases;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

/**
 * 调试命令：/dtdebug（默认关闭，由 {@link DTEmeraldCraftConfig#ENABLE_DEBUG_COMMAND} 控制）。
 *
 * <p>用于在实机环境中定位「EC 生物群系里 DT 树未生成」类问题。
 * 命令会输出以下诊断信息到游戏日志（DEBUG/INFO 级）与聊天框：
 * <ol>
 *   <li>本附属四种 species 是否成功注册（遍历 Species.REGISTRY）；</li>
 *   <li>DT 的 BiomeDatabase（默认维度数据库）当前填充状态；</li>
 *   <li>四个 EC 生物群系在 BiomeDatabase 里是否被识别、species selector 是否为 null；</li>
 *   <li>四种 species 对常见地表方块的土壤判定；</li>
 *   <li>世界生成判定链模拟（DTConfigs 开关、维度黑名单、density/heightmap/forestness）。</li>
 * </ol>
 *
 * <p>配合 DT 自带的 config {@code [debug] debug=true}（地面彩色混凝土圈标记判定结果）使用：
 * 白=GENERATED、黑=NO_TREE、黄=UNHANDLED_BIOME、棕=FAIL_SOIL、蓝=FAIL_CHANCE、
 * 红=FAIL_GENERATION、紫=NO_GROUND、灰=ALREADY_GENERATED。
 *
 * @author night
 */
public final class DTDebugCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    /** 四个 EC 生物群系注册键，与 world_gen/default.json 的 select.name 一一对应。 */
    private static final String[] EC_BIOMES = {
            "ginkgo_forest",
            "palm_beach",
            "xanadu",
            "purpuraceus_swamp"
    };

    /** 四种 species 注册名（trees/.../species/ 下的文件名）。 */
    private static final String[] MY_SPECIES = {
            "ginkgo",
            "palm",
            "peach",
            "purpuraceus"
    };

    private DTDebugCommand() {
        throw new IllegalStateException("Utility class");
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dtdebug")
                .executes(ctx -> run(ctx.getSource())));
    }

    private static int run(CommandSourceStack source) {
        log("========== DT-EmeraldCraft 调试 ==========");

        dumpSpeciesRegistry();
        dumpBiomeDatabase();
        dumpEcBiomeEntries();
        dumpSoilAcceptance();
        dumpWorldGenSimulation(source);

        log("========== 调试结束 ==========");
        source.sendSuccess(() -> Component.literal("DT 调试信息已输出到启动器日志 (logs/debug.log / latest.log)"), false);
        return 1;
    }

    /** 第一步：确认四种 species 是否注册成功。 */
    private static void dumpSpeciesRegistry() {
        log("[1/3] Species 注册检查：");
        Set<Species> all = Species.REGISTRY.getAll();
        int myCount = 0;
        for (String name : MY_SPECIES) {
            ResourceLocation rl = DynamicTreesEmeraldCraft.location(name);
            Species sp = Species.REGISTRY.get(rl);
            if (sp != null) {
                myCount++;
                log("  找到 species: " + rl + " (类型=" + sp.getClass().getSimpleName()
                        + ", family=" + (sp.getFamily() != null ? sp.getFamily().getRegistryName() : "null") + ")");
            } else {
                log("  [缺失] 未找到 species: " + rl);
            }
        }
        log("  合计注册 " + myCount + "/" + MY_SPECIES.length + " 个本附属 species；DT 全局 species 总数=" + all.size());
    }

    /** 第二步：确认 BiomeDatabase 的填充状态与 populator 条目数量。 */
    private static void dumpBiomeDatabase() {
        log("[2/3] BiomeDatabase 状态：");
        BiomeDatabase defDb = BiomeDatabases.getDefault();
        log("  默认数据库 isPopulated=" + defDb.isPopulated());

        // jsonEntries / entries 是 private 字段，通过反射读取其数量，用于判断 default.json 是否被解析进数据库。
        try {
            Field jsonEntriesField = BiomeDatabase.class.getDeclaredField("jsonEntries");
            jsonEntriesField.setAccessible(true);
            Object jsonEntries = jsonEntriesField.get(defDb);
            if (jsonEntries instanceof Map<?, ?> m) {
                log("  jsonEntries 数量=" + m.size() + "（DT 内置 default.json + 本附属 default.json 的 populator 条目总数）");
            }

            Field entriesField = BiomeDatabase.class.getDeclaredField("entries");
            entriesField.setAccessible(true);
            Object entries = entriesField.get(defDb);
            if (entries instanceof Map<?, ?> m) {
                log("  entries 数量=" + m.size() + "（已被求值的生物群系数）");
            }
        } catch (Exception e) {
            log("  [反射失败] 无法读取 BiomeDatabase 内部字段: " + e);
        }
    }

    /** 第三步：确认四个 EC 生物群系在 BiomeDatabase 里的 species / chance / density 三个选择器。 */
    private static void dumpEcBiomeEntries() {
        log("[3/3] EC 生物群系 -> 三个选择器检查：");
        BiomeDatabase defDb = BiomeDatabases.getDefault();
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
        for (String biomeName : EC_BIOMES) {
            ResourceLocation rl = new ResourceLocation("emeraldcraft", biomeName);
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, rl);
            try {
                BiomeDatabase.Entry entry = defDb.getEntry(key);
                log("  " + rl + ":");

                // 1) species selector
                BiomePropertySelectors.SpeciesSelector speciesSelector = entry.getSpeciesSelector();
                Species sampledSpecies = sampleSpecies(speciesSelector);
                log("      speciesSelector=" + (speciesSelector == null ? "null" : speciesSelector.getClass().getSimpleName())
                        + " -> " + (sampledSpecies != null ? sampledSpecies.getRegistryName() : "null(不生成)"));

                // 2) chance selector（采样三次，观察是 OK / CANCEL / UNHANDLED）
                BiomePropertySelectors.ChanceSelector chanceSelector = entry.getChanceSelector();
                StringBuilder chanceDesc = new StringBuilder();
                if (sampledSpecies != null) {
                    for (int i = 0; i < 5; i++) {
                        BiomePropertySelectors.Chance c = chanceSelector.getChance(random, sampledSpecies, 8);
                        if (chanceDesc.length() > 0) chanceDesc.append(',');
                        chanceDesc.append(c);
                    }
                }
                log("      chanceSelector=" + (chanceSelector == null ? "null" : chanceSelector.getClass().getSimpleName())
                        + " -> 采样5次=[" + chanceDesc + "]");

                // 3) density selector（采样一次）
                BiomePropertySelectors.DensitySelector densitySelector = entry.getDensitySelector();
                double density = 0;
                try {
                    density = densitySelector.getDensity(random, 1.0);
                } catch (Exception ignored) {
                }
                log("      densitySelector=" + (densitySelector == null ? "null" : densitySelector.getClass().getSimpleName())
                        + " -> 采样=" + density);

            } catch (Exception e) {
                log("  " + rl + " -> [异常] " + e);
            }
        }
    }

    /** 从 species selector 采样一个 species（用于 chance/density 采样需要 Species 实例）。 */
    private static Species sampleSpecies(BiomePropertySelectors.SpeciesSelector selector) {
        if (selector == null) {
            return null;
        }
        try {
            net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.ZERO;
            net.minecraft.world.level.block.state.BlockState state =
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
            BiomePropertySelectors.SpeciesSelection sel = selector.getSpecies(pos, state, random);
            if (sel != null && sel.isHandled()) {
                return sel.getSpecies();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 第四步：对每种 species 测试常见地表方块的土壤判定（isAcceptableSoilForWorldgen）。 */
    private static void dumpSoilAcceptance() {
        log("[4/4] 土壤判定测试（isAcceptableSoilForWorldgen）：");
        // 常见地表方块，覆盖 dirt_like / nether / fungus / sand / moss 等场景。
        // 特别加入 xanadu 的 moss_block 与 purpuraceus_swamp 的 purpuraceus_nylium。
        net.minecraft.world.level.block.Block[] testBlocks = {
                net.minecraft.world.level.block.Blocks.GRASS_BLOCK,
                net.minecraft.world.level.block.Blocks.DIRT,
                net.minecraft.world.level.block.Blocks.COARSE_DIRT,
                net.minecraft.world.level.block.Blocks.PODZOL,
                net.minecraft.world.level.block.Blocks.MYCELIUM,
                net.minecraft.world.level.block.Blocks.SAND,
                net.minecraft.world.level.block.Blocks.GRAVEL,
                net.minecraft.world.level.block.Blocks.NETHERRACK,
                net.minecraft.world.level.block.Blocks.CRIMSON_NYLIUM,
                net.minecraft.world.level.block.Blocks.WARPED_NYLIUM,
                net.minecraft.world.level.block.Blocks.SOUL_SOIL,
                net.minecraft.world.level.block.Blocks.MOSS_BLOCK,
                // EC 自定义菌岩（purpuraceus_swamp 地表），运行时通过 ForgeRegistries 解析
                net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                        new net.minecraft.resources.ResourceLocation("emeraldcraft", "purpuraceus_nylium"))
        };
        for (String name : MY_SPECIES) {
            Species sp = Species.REGISTRY.get(DynamicTreesEmeraldCraft.location(name));
            if (sp == null) {
                continue;
            }
            StringBuilder accepted = new StringBuilder();
            for (net.minecraft.world.level.block.Block block : testBlocks) {
                try {
                    boolean ok = sp.isAcceptableSoilForWorldgen(block.defaultBlockState());
                    if (ok) {
                        if (accepted.length() > 0) accepted.append(", ");
                        accepted.append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block));
                    }
                } catch (Exception ignored) {
                }
            }
            log("  " + sp.getRegistryName() + " 接受的常见地表=[" + accepted + "]");
        }

        // 附加诊断：确认关键方块是否已登记进 DT 的 dirtMap（SoilHelper.isSoilRegistered）。
        log("  -- dirtMap 注册状态检查（SoilHelper.isSoilRegistered）--");
        checkSoilRegistered("emeraldcraft:purpuraceus_nylium");
        checkSoilRegistered("minecraft:moss_block");
        checkSoilRegistered("minecraft:crimson_nylium");
    }

    /**
     * 第五步：模拟 DynamicTreeFeature#generateTree 的完整判定链，定位「树为何不生成」。
     *
     * <p>DT 1.4.11 的世界生成入口是 {@code DynamicTreeFeature#place}，它通过
     * {@code BiomeRadiusCoordinator#getRadiusAtCoords}（由 density selector 决定泊松圆盘半径）
     * 生成 PoissonDisc，再对每个 disc 依次判定 species / isValid / soil / chance 后才真正
     * generate 一棵树。这里复现每一步判定，输出每个生物群系在每一关的结果。
     */
    private static void dumpWorldGenSimulation(CommandSourceStack source) {
        log("[5/5] 世界生成判定链模拟（DynamicTreeFeature 视角）：");

        // 0) DT 世界生成总开关与调试开关（config/dynamictrees-common.toml）
        try {
            log("  DTConfigs.WORLD_GEN（config 里 [world] worldGen）= "
                    + com.ferreusveritas.dynamictrees.init.DTConfigs.WORLD_GEN.get()
                    + "（false 时 DT 完全不在世界生成任何树）");
            log("  DTConfigs.WORLD_GEN_DEBUG（config 里 [debug] debug）= "
                    + com.ferreusveritas.dynamictrees.init.DTConfigs.WORLD_GEN_DEBUG.get()
                    + "（true 时 DT 会在地面用彩色混凝土圈标记每一处判定结果，是定位本问题的最强手段）");
        } catch (Throwable t) {
            log("  [异常] 读取 DTConfigs 失败: " + t);
        }

        // 1) 维度黑名单：EC 树都在 overworld，若 overworld 被黑名单则全部不生成
        try {
            net.minecraft.resources.ResourceLocation overworld = net.minecraft.world.level.Level.OVERWORLD.location();
            log("  overworld 维度是否被 DT 黑名单: " + com.ferreusveritas.dynamictrees.worldgen.BiomeDatabases.isBlacklisted(overworld)
                    + "（维度=" + overworld + "）");
        } catch (Throwable t) {
            log("  [异常] 黑名单检测失败: " + t);
        }

        // 2) 逐生物群系：density / heightmap / forestness 实测
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
        for (String biomeName : EC_BIOMES) {
            ResourceLocation rl = new ResourceLocation("emeraldcraft", biomeName);
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, rl);
            try {
                net.minecraft.core.Holder<Biome> holder = source.getServer().registryAccess()
                        .registryOrThrow(Registries.BIOME).getHolder(key).orElse(null);
                if (holder == null) {
                    log("  " + rl + " -> [无法解析 Holder，生物群系可能未生成]");
                    continue;
                }

                BiomeDatabase defDb = BiomeDatabases.getDefault();
                StringBuilder sb = new StringBuilder("  " + rl + ":");

                // density selector 采样 8 次，观察是否真的接近 1.0
                double densityMin = Double.MAX_VALUE, densityMax = -Double.MAX_VALUE;
                double densitySum = 0;
                try {
                    for (int i = 0; i < 8; i++) {
                        double d = defDb.getDensitySelector(holder).getDensity(random, 0.5);
                        densityMin = Math.min(densityMin, d);
                        densityMax = Math.max(densityMax, d);
                        densitySum += d;
                    }
                    sb.append("\n      density 采样8次=[min=").append(String.format("%.3f", densityMin))
                            .append(", max=").append(String.format("%.3f", densityMax))
                            .append(", avg=").append(String.format("%.3f", densitySum / 8)).append("]");
                    // 复现 BiomeRadiusCoordinator 的 density→半径公式：(1-density)*9 → 半径约 2~11
                    double radius = Math.max(2, (int) ((1.0 - (densitySum / 8)) * 9.0) + 2);
                    sb.append("（估算泊松半径≈").append((int) radius).append(" 格，越小越密）");
                } catch (Throwable t) {
                    sb.append("\n      density [异常] ").append(t);
                }

                // heightmap：EC 群系若用非标准 heightmap，GroundFinder 可能找不到地面
                try {
                    sb.append("\n      heightmap=").append(defDb.getHeightmap(holder));
                } catch (Throwable t) {
                    sb.append("\n      heightmap [异常] ").append(t);
                }

                // forestness：DT 森林度，仅影响某些 selector，记录即可
                try {
                    sb.append("\n      forestness=").append(defDb.getForestness(holder));
                } catch (Throwable t) {
                    sb.append("\n      forestness [异常] ").append(t);
                }

                log(sb.toString());
            } catch (Exception e) {
                log("  " + rl + " -> [异常] " + e);
            }
        }

        // 3) 提示：最权威的验证方式（开启 DT 世界生成调试圈）
        log("  -- 建议 --");
        log("    打开 config/dynamictrees-common.toml，把 [debug] 段的 debug 改为 true，");
        log("    然后飞到 EC 生物群系观察地面：");
        log("      彩色混凝土圈 = DT 判定树不该生成的位置（对应 FAIL_SOIL/FAIL_CHANCE/NO_TREE 等）");
        log("      若 EC 群系地面完全没有任何圈 = DynamicTreeFeature 根本没在 EC 群系运行");
    }

    /** 检查单个方块是否已登记进 DT dirtMap，并输出结果。 */
    private static void checkSoilRegistered(String blockId) {
        net.minecraft.world.level.block.Block block = null;
        try {
            // 注意：必须用完整命名空间 ID（如 emeraldcraft:purpuraceus_nylium），
            // 否则 new ResourceLocation("xxx") 会默认补 minecraft: 前缀，查错方块。
            net.minecraft.resources.ResourceLocation rl = new net.minecraft.resources.ResourceLocation(blockId);
            block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(rl);
        } catch (Exception ignored) {
        }
        if (block == null) {
            log("    [缺失方块] " + blockId + " 未在 ForgeRegistries.BLOCKS 中找到");
            return;
        }
        boolean registered = com.ferreusveritas.dynamictrees.block.rooty.SoilHelper.isSoilRegistered(block);
        log("    " + blockId + " -> dirtMap 已注册=" + registered);
    }

    /** 尽量把 SpeciesSelector 描述成可读字符串。 */
    private static void log(String msg) {
        // 输出到游戏日志（信息级），确保在 debug.log 与 latest.log 中可见。
        LOGGER.info("[DTEC-debug] " + msg);
    }
}
