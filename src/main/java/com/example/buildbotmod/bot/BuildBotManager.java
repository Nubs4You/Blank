package com.example.buildbotmod.bot;

import com.example.buildbotmod.BuildBotMod;
import com.example.buildbotmod.config.BuildBotConfig;
import com.example.buildbotmod.llm.LlmBuildPlanner;
import com.example.buildbotmod.model.BuildPlan;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BuildBotManager {
    private final BuildBotConfig buildBotConfig;
    private final LlmBuildPlanner llmBuildPlanner;
    private final Map<UUID, BuilderSession> builderSessionByPlayerId;

    public BuildBotManager(final BuildBotConfig buildBotConfig) {
        this.buildBotConfig = buildBotConfig;
        this.llmBuildPlanner = new LlmBuildPlanner(buildBotConfig);
        this.builderSessionByPlayerId = new ConcurrentHashMap<>();
    }

    public void startBuild(final ServerPlayerEntity player, final String prompt) {
        if (player == null) {
            return;
        }
        if (prompt == null || prompt.isBlank()) {
            player.sendMessage(Text.literal("Build prompt cannot be empty."), false);
            return;
        }

        final BuildPlan buildPlan = this.llmBuildPlanner.createPlan(prompt);
        if (buildPlan == null || buildPlan.getInstructions() == null || buildPlan.getInstructions().isEmpty()) {
            player.sendMessage(Text.literal("No valid build instructions were generated."), false);
            return;
        }

        final BuilderSession existingBuilderSession = this.builderSessionByPlayerId.remove(player.getUuid());
        if (existingBuilderSession != null) {
            existingBuilderSession.stop();
        }

        final BuilderSession builderSession = new BuilderSession(player, buildPlan, this.buildBotConfig);
        this.builderSessionByPlayerId.put(player.getUuid(), builderSession);
        player.sendMessage(Text.literal("Builder bot spawned. Plan: " + buildPlan.getSummary() + " with " + buildPlan.getInstructions().size() + " blocks."), false);
    }

    public void tick(final MinecraftServer minecraftServer) {
        if (minecraftServer == null || this.builderSessionByPlayerId.isEmpty()) {
            return;
        }

        this.builderSessionByPlayerId.values().removeIf(builderSession -> {
            final boolean stillRunning = builderSession.tick();
            return !stillRunning;
        });
    }

    public void shutdown() {
        for (final BuilderSession builderSession : this.builderSessionByPlayerId.values()) {
            builderSession.stop();
        }
        this.builderSessionByPlayerId.clear();
        BuildBotMod.LOGGER.info("BuildBotManager shut down.");
    }
}
