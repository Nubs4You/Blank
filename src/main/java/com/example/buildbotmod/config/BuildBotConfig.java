package com.example.buildbotmod.config;

import com.example.buildbotmod.BuildBotMod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
@Setter
@NoArgsConstructor
public final class BuildBotConfig {
    private String llmEndpoint = "https://api.openai.com/v1/chat/completions";
    private String llmApiKey = "PUT_YOUR_API_KEY_HERE";
    private String llmModel = "gpt-4o-mini";
    private int blocksPerTick = 64;
    private int maxRadius = 128;

    public static BuildBotConfig load() {
        final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("buildbotmod.properties");
        final BuildBotConfig buildBotConfig = new BuildBotConfig();

        if (!Files.exists(configPath)) {
            buildBotConfig.save(configPath);
            return buildBotConfig;
        }

        try {
            final String fileContent = Files.readString(configPath, StandardCharsets.UTF_8);
            final String[] lines = fileContent.split("\\R");
            for (final String line : lines) {
                if (line == null || line.isBlank() || !line.contains("=")) {
                    continue;
                }
                final String[] keyValue = line.split("=", 2);
                if (keyValue.length != 2) {
                    continue;
                }
                buildBotConfig.apply(keyValue[0].trim(), keyValue[1].trim());
            }
        } catch (final IOException ioException) {
            BuildBotMod.LOGGER.error("Failed to read BuildBot config, using defaults.", ioException);
        }

        return buildBotConfig;
    }

    private void apply(final String key, final String value) {
        if (key == null || value == null) {
            return;
        }
        switch (key) {
            case "llmEndpoint" -> this.llmEndpoint = value;
            case "llmApiKey" -> this.llmApiKey = value;
            case "llmModel" -> this.llmModel = value;
            case "blocksPerTick" -> this.blocksPerTick = parseIntOrDefault(value, this.blocksPerTick);
            case "maxRadius" -> this.maxRadius = parseIntOrDefault(value, this.maxRadius);
            default -> BuildBotMod.LOGGER.warn("Unknown BuildBot config key: {}", key);
        }
    }

    private static int parseIntOrDefault(final String value, final int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException numberFormatException) {
            return fallback;
        }
    }

    private void save(final Path configPath) {
        final StringBuilder outputBuilder = new StringBuilder();
        outputBuilder.append("llmEndpoint=").append(this.llmEndpoint).append('\n');
        outputBuilder.append("llmApiKey=").append(this.llmApiKey).append('\n');
        outputBuilder.append("llmModel=").append(this.llmModel).append('\n');
        outputBuilder.append("blocksPerTick=").append(this.blocksPerTick).append('\n');
        outputBuilder.append("maxRadius=").append(this.maxRadius).append('\n');

        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, outputBuilder.toString(), StandardCharsets.UTF_8);
        } catch (final IOException ioException) {
            BuildBotMod.LOGGER.error("Failed to save BuildBot config file.", ioException);
        }
    }
}
