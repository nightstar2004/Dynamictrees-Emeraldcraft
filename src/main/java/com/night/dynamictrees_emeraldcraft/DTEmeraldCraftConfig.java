package com.night.dynamictrees_emeraldcraft;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 模组配置。目前仅提供调试命令开关：
 * <ul>
 *   <li>{@code [general] enableDebugCommand}：是否注册 /dtdebug 调试命令，默认 {@code false}。</li>
 * </ul>
 *
 * <p>调试命令仅用于排查「EC 生物群系动态树未生成」类问题，正常游玩无需开启。
 *
 * @author night
 */
public final class DTEmeraldCraftConfig {

    /** 是否注册 /dtdebug 调试命令（默认关闭）。 */
    public static final ForgeConfigSpec.BooleanValue ENABLE_DEBUG_COMMAND;

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General settings").push("general");
        ENABLE_DEBUG_COMMAND = builder
                .comment("Register the /dtdebug diagnostic command (world-gen troubleshooting).",
                         "Keep false for normal gameplay.")
                .define("enableDebugCommand", false);
        builder.pop();

        SPEC = builder.build();
    }

    private DTEmeraldCraftConfig() {
        throw new IllegalStateException("Utility class");
    }
}
