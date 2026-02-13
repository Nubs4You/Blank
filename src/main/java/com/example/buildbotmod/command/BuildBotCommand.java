package com.example.buildbotmod.command;

import com.example.buildbotmod.BuildBotMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class BuildBotCommand {
    private BuildBotCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(final CommandDispatcher<ServerCommandSource> commandDispatcher) {
        commandDispatcher.register(CommandManager.literal("buildbot")
                .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                        .executes(commandContext -> {
                            final ServerCommandSource serverCommandSource = commandContext.getSource();
                            final ServerPlayerEntity player = serverCommandSource.getPlayer();
                            if (player == null) {
                                serverCommandSource.sendError(Text.literal("Only players can run this command."));
                                return 0;
                            }

                            final String prompt = StringArgumentType.getString(commandContext, "prompt");
                            if (prompt == null || prompt.isBlank()) {
                                serverCommandSource.sendError(Text.literal("Prompt cannot be empty."));
                                return 0;
                            }

                            BuildBotMod.getInstance().getBuildBotManager().startBuild(player, prompt);
                            return 1;
                        }))
                .executes(commandContext -> {
                    commandContext.getSource().sendFeedback(() -> Text.literal("Usage: /buildbot <natural language build prompt>"), false);
                    return 1;
                }));
    }
}
