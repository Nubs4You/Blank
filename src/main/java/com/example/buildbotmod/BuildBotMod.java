package com.example.buildbotmod;

import com.example.buildbotmod.bot.BuildBotManager;
import com.example.buildbotmod.command.BuildBotCommand;
import com.example.buildbotmod.config.BuildBotConfig;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public final class BuildBotMod implements ModInitializer {
    public static final String MOD_ID = "buildbotmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Getter
    private static BuildBotMod instance;

    private BuildBotConfig buildBotConfig;
    private BuildBotManager buildBotManager;

    @Override
    public void onInitialize() {
        BuildBotMod.instance = this;
        this.buildBotConfig = BuildBotConfig.load();
        this.buildBotManager = new BuildBotManager(this.buildBotConfig);

        BuildBotCommand.register();

        ServerTickEvents.END_SERVER_TICK.register(server -> this.buildBotManager.tick(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> this.buildBotManager.shutdown());

        LOGGER.info("BuildBot Mod initialized.");
    }
}
