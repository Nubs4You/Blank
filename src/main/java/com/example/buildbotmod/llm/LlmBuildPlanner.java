package com.example.buildbotmod.llm;

import com.example.buildbotmod.BuildBotMod;
import com.example.buildbotmod.config.BuildBotConfig;
import com.example.buildbotmod.model.BuildInstruction;
import com.example.buildbotmod.model.BuildPlan;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LlmBuildPlanner {
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{\\s*\"x\"\\s*:\\s*(-?\\d+)\\s*,\\s*\"y\"\\s*:\\s*(-?\\d+)\\s*,\\s*\"z\"\\s*:\\s*(-?\\d+)\\s*,\\s*\"block\"\\s*:\\s*\"([a-z0-9_:.\\-]+)\"\\s*}");

    private final BuildBotConfig buildBotConfig;
    private final HttpClient httpClient;

    public LlmBuildPlanner(final BuildBotConfig buildBotConfig) {
        this.buildBotConfig = buildBotConfig;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public BuildPlan createPlan(final String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return fallbackPlan("Default stone platform", 32, 2, 32, "minecraft:stone");
        }

        if (this.buildBotConfig.getLlmApiKey() == null || this.buildBotConfig.getLlmApiKey().isBlank() || "PUT_YOUR_API_KEY_HERE".equals(this.buildBotConfig.getLlmApiKey())) {
            BuildBotMod.LOGGER.warn("LLM API key is not configured. Falling back to procedural build plan.");
            return proceduralPlanFromPrompt(prompt);
        }

        try {
            final String requestBody = buildRequestBody(prompt);
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(this.buildBotConfig.getLlmEndpoint()))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + this.buildBotConfig.getLlmApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            final HttpResponse<String> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                BuildBotMod.LOGGER.error("LLM request failed with status {}: {}", response.statusCode(), response.body());
                return proceduralPlanFromPrompt(prompt);
            }

            final String llmContent = extractMessageContent(response.body());
            if (llmContent == null || llmContent.isBlank()) {
                return proceduralPlanFromPrompt(prompt);
            }

            final BuildPlan parsedPlan = parseInstructionJson(llmContent);
            if (parsedPlan.getInstructions().isEmpty()) {
                return proceduralPlanFromPrompt(prompt);
            }
            return parsedPlan;
        } catch (final InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            BuildBotMod.LOGGER.error("LLM call was interrupted. Using fallback plan.", interruptedException);
            return proceduralPlanFromPrompt(prompt);
        } catch (final IOException ioException) {
            BuildBotMod.LOGGER.error("Failed to call LLM. Using fallback plan.", ioException);
            return proceduralPlanFromPrompt(prompt);
        }
    }

    private static String buildRequestBody(final String prompt) {
        final String escapedPrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        return """
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "system",
                      "content": "You are a Minecraft building planner. Return only JSON with format: {\\\"summary\\\":\\\"...\\\",\\\"instructions\\\":[{\\\"x\\\":0,\\\"y\\\":0,\\\"z\\\":0,\\\"block\\\":\\\"minecraft:stone\\\"}]}."
                    },
                    {
                      "role": "user",
                      "content": "Create a large build plan for this request: %s"
                    }
                  ],
                  "temperature": 0.2
                }
                """.formatted(BuildBotMod.getInstance().getBuildBotConfig().getLlmModel(), escapedPrompt);
    }

    private static String extractMessageContent(final String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        final String marker = "\"content\":";
        final int markerIndex = responseBody.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }

        final int firstQuote = responseBody.indexOf('"', markerIndex + marker.length());
        if (firstQuote < 0) {
            return null;
        }

        final StringBuilder contentBuilder = new StringBuilder();
        boolean escaped = false;
        for (int index = firstQuote + 1; index < responseBody.length(); index++) {
            final char character = responseBody.charAt(index);
            if (escaped) {
                if (character == 'n') {
                    contentBuilder.append('\n');
                } else {
                    contentBuilder.append(character);
                }
                escaped = false;
                continue;
            }
            if (character == '\\') {
                escaped = true;
                continue;
            }
            if (character == '"') {
                return contentBuilder.toString();
            }
            contentBuilder.append(character);
        }
        return null;
    }

    private static BuildPlan parseInstructionJson(final String llmJsonFragment) {
        final List<BuildInstruction> buildInstructions = new ArrayList<>();
        final Matcher matcher = JSON_BLOCK_PATTERN.matcher(llmJsonFragment);

        while (matcher.find()) {
            final int x = Integer.parseInt(matcher.group(1));
            final int y = Integer.parseInt(matcher.group(2));
            final int z = Integer.parseInt(matcher.group(3));
            final String blockId = matcher.group(4);
            if (isKnownBlock(blockId)) {
                buildInstructions.add(new BuildInstruction(x, y, z, blockId));
            }
        }

        final BuildPlan buildPlan = new BuildPlan();
        buildPlan.setSummary("LLM-generated plan");
        buildPlan.setInstructions(buildInstructions);
        return buildPlan;
    }

    private static BuildPlan proceduralPlanFromPrompt(final String prompt) {
        final String lowercasePrompt = prompt.toLowerCase(Locale.ROOT);
        if (lowercasePrompt.contains("tower")) {
            return fallbackPlan("Fallback tower", 16, 30, 16, "minecraft:stone_bricks");
        }
        if (lowercasePrompt.contains("pyramid")) {
            return pyramidPlan("Fallback pyramid", 40, "minecraft:sandstone");
        }
        return fallbackPlan("Fallback platform", 48, 4, 48, "minecraft:oak_planks");
    }

    private static BuildPlan fallbackPlan(final String summary, final int width, final int height, final int depth, final String blockId) {
        final List<BuildInstruction> buildInstructions = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                for (int y = 0; y < height; y++) {
                    if (y > 0 && x > 0 && x < width - 1 && z > 0 && z < depth - 1) {
                        continue;
                    }
                    buildInstructions.add(new BuildInstruction(x, y, z, blockId));
                }
            }
        }
        return new BuildPlan(summary, buildInstructions);
    }

    private static BuildPlan pyramidPlan(final String summary, final int size, final String blockId) {
        final List<BuildInstruction> buildInstructions = new ArrayList<>();
        int layer = 0;
        int currentSize = size;
        while (currentSize > 0) {
            for (int x = 0; x < currentSize; x++) {
                for (int z = 0; z < currentSize; z++) {
                    buildInstructions.add(new BuildInstruction(x + layer, layer, z + layer, blockId));
                }
            }
            currentSize -= 2;
            layer++;
        }
        return new BuildPlan(summary, buildInstructions);
    }

    private static boolean isKnownBlock(final String blockId) {
        try {
            final Identifier identifier = Identifier.tryParse(blockId);
            if (identifier == null) {
                return false;
            }
            final Block block = Registries.BLOCK.get(identifier);
            return block != null;
        } catch (final RuntimeException exception) {
            return false;
        }
    }
}
