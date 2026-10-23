package com.night.dynamictrees_emeraldcraft;

import com.ferreusveritas.dynamictrees.api.GatherDataHelper;
import com.ferreusveritas.dynamictrees.api.registry.RegistryHandler;
import com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties;
import com.ferreusveritas.dynamictrees.block.rooty.SoilProperties;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Dynamic Trees for EmeraldCraft 主类。
 *
 * <p>四种 EmeraldCraft 原生树（银杏、棕榈、桃树、紫菌木）的动态树行为
 * 全部由 DT 1.20.1 数据驱动系统完成：DT 资源加载器在启动阶段读取
 * {@code trees/dynamictrees_emeraldcraft/} 目录下的 JSON（families / species /
 * leaves_properties / soil_properties / world_gen / jo_codes）并自动完成注册。
 * Java 侧仅负责：RegistryHandler 初始化、配置注册与调试命令开关。
 *
 * @author night
 */
@Mod(DynamicTreesEmeraldCraft.MOD_ID)
public class DynamicTreesEmeraldCraft {

    public static final String MOD_ID = "dynamictrees_emeraldcraft";

    public DynamicTreesEmeraldCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::gatherData);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DTEmeraldCraftConfig.SPEC);

        MinecraftForge.EVENT_BUS.register(this);

        RegistryHandler.setup(MOD_ID);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        DTEmeraldCraftRegistries.setup();
    }

    private void gatherData(final GatherDataEvent event) {
        GatherDataHelper.gatherAllData(MOD_ID, event,
                SoilProperties.REGISTRY,
                Family.REGISTRY,
                Species.REGISTRY,
                LeavesProperties.REGISTRY);
    }

    /** 调试命令按配置开关注册（config/dynamictrees_emeraldcraft-common.toml）。 */
    @SubscribeEvent
    public void onRegisterCommands(final RegisterCommandsEvent event) {
        if (DTEmeraldCraftConfig.ENABLE_DEBUG_COMMAND.get()) {
            DTDebugCommand.register(event.getDispatcher());
        }
    }

    public static ResourceLocation location(final String path) {
        return new ResourceLocation(MOD_ID, path);
    }

}
