package com.night.dynamictrees_emeraldcraft;

import net.minecraft.resources.ResourceLocation;

/**
 * 注册表持有类：集中声明四种树种的 {@link ResourceLocation} 常量。
 *
 * <p>所有 Family / Species / LeavesProperties / SoilProperties 实例均由 DT 资源加载器
 * 在 trees/ JSON 加载阶段自动创建并注册，无需 Java 代码注册。
 *
 * <p>纹理说明：EmeraldCraft 源码为 MIT 协议，其资产（含原版贴图）为 All Rights Reserved。
 * 本附属的动态树干使用自行绘制的纹理（assets/dynamictrees_emeraldcraft/textures/block/），
 * 通过 family JSON 的 {@code texture_overrides} 绑定到 DT 树干模型。
 *
 * <h3>四种原生树与 EC 方块 ID 对照</h3>
 * <pre>
 * 银杏   Ginkgo       原木 emeraldcraft:ginkgo_log          树苗 emeraldcraft:ginkgo_sapling
 * 棕榈   Palm         原木 emeraldcraft:palm_log            树苗 emeraldcraft:palm_sapling
 * 桃树   Peach        原木 emeraldcraft:peach_log           树苗 emeraldcraft:peach_sapling
 * 紫菌木 Purpuraceus  菌茎 emeraldcraft:purpuraceus_stem    菌体 emeraldcraft:purpuraceus_fungus
 * </pre>
 *
 * @author night
 */
public final class DTEmeraldCraftRegistries {

    /** 银杏树种 —— trees/dynamictrees_emeraldcraft/species/ginkgo.json */
    public static final ResourceLocation GINKGO = location("ginkgo");

    /** 棕榈树种（dynamictrees:palm 类型）—— species/palm.json */
    public static final ResourceLocation PALM = location("palm");

    /** 桃树树种 —— species/peach.json */
    public static final ResourceLocation PEACH = location("peach");

    /** 紫菌木树种（dynamictrees:nether_fungus 类型）—— species/purpuraceus.json */
    public static final ResourceLocation PURPURACEUS = location("purpuraceus");

    public static void setup() {
        // 纯 JSON 数据驱动，无 Java 侧注册：
        // purpuraceus_nylium 的土壤注册依赖 soil_properties/purpuraceus_nylium.json，
        // 由 SoilPropertiesResourceLoader 在资源加载阶段自动完成（生成 rooty 方块 + 登记 dirtMap）。
        // 切勿在 commonSetup 用代码注册土壤：此时方块注册阶段已结束，
        // SoilHelper.registerSoil 触发的 generateBlock 会导致 DeferredRegister 崩溃。
    }

    private static ResourceLocation location(final String path) {
        return DynamicTreesEmeraldCraft.location(path);
    }

    private DTEmeraldCraftRegistries() {
        throw new IllegalStateException("Utility class");
    }

}
