package com.example.buildbotmod.bot;

import com.example.buildbotmod.config.BuildBotConfig;
import com.example.buildbotmod.model.BuildInstruction;
import com.example.buildbotmod.model.BuildPlan;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public final class BuilderSession {
    private final ServerPlayerEntity player;
    private final BuildPlan buildPlan;
    private final BuildBotConfig buildBotConfig;
    private final BlockPos origin;

    @Getter
    private final ArmorStandEntity botEntity;

    private int currentIndex;
    private boolean stopped;

    public BuilderSession(final ServerPlayerEntity player, final BuildPlan buildPlan, final BuildBotConfig buildBotConfig) {
        this.player = player;
        this.buildPlan = buildPlan;
        this.buildBotConfig = buildBotConfig;
        this.origin = player.getBlockPos().add(2, 0, 2);

        this.botEntity = new ArmorStandEntity(EntityType.ARMOR_STAND, player.getWorld());
        this.botEntity.setInvisible(false);
        this.botEntity.setInvulnerable(true);
        this.botEntity.setCustomName(Text.literal("Builder Bot"));
        this.botEntity.setCustomNameVisible(true);
        this.botEntity.setNoGravity(true);
        this.botEntity.refreshPositionAndAngles(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D, 0.0F, 0.0F);
        player.getWorld().spawnEntity(this.botEntity);
    }

    public boolean tick() {
        if (this.stopped) {
            return false;
        }
        if (this.player.isDisconnected() || this.player.getWorld() == null) {
            this.stop();
            return false;
        }

        final List<BuildInstruction> buildInstructions = this.buildPlan.getInstructions();
        if (buildInstructions == null || buildInstructions.isEmpty()) {
            this.player.sendMessage(Text.literal("Builder bot stopped: empty instruction list."), false);
            this.stop();
            return false;
        }

        final World world = this.player.getWorld();
        final int maxPlacementsThisTick = Math.max(1, this.buildBotConfig.getBlocksPerTick());
        int placements = 0;

        while (this.currentIndex < buildInstructions.size() && placements < maxPlacementsThisTick) {
            final BuildInstruction buildInstruction = buildInstructions.get(this.currentIndex);
            this.currentIndex++;
            if (buildInstruction == null) {
                continue;
            }

            if (Math.abs(buildInstruction.getX()) > this.buildBotConfig.getMaxRadius() || Math.abs(buildInstruction.getZ()) > this.buildBotConfig.getMaxRadius()) {
                continue;
            }

            final BlockPos targetPosition = this.origin.add(buildInstruction.getX(), buildInstruction.getY(), buildInstruction.getZ());
            final Identifier identifier = Identifier.tryParse(buildInstruction.getBlockId());
            if (identifier == null) {
                continue;
            }
            final Block block = Registries.BLOCK.get(identifier);
            if (block == null) {
                continue;
            }
            final BlockState blockState = block.getDefaultState();
            world.setBlockState(targetPosition, blockState, 3);
            final Vec3d botTargetPosition = new Vec3d(targetPosition.getX() + 0.5D, targetPosition.getY() + 1.0D, targetPosition.getZ() + 0.5D);
            this.botEntity.refreshPositionAndAngles(botTargetPosition.x, botTargetPosition.y, botTargetPosition.z, this.botEntity.getYaw(), this.botEntity.getPitch());
            placements++;
        }

        if (this.currentIndex >= buildInstructions.size()) {
            this.player.sendMessage(Text.literal("Builder bot completed the build plan."), false);
            this.stop();
            return false;
        }

        if (this.currentIndex % 200 == 0) {
            this.player.sendMessage(Text.literal("Builder bot progress: " + this.currentIndex + " / " + buildInstructions.size()), true);
        }

        return true;
    }

    public void stop() {
        this.stopped = true;
        if (this.botEntity != null && this.botEntity.isAlive()) {
            this.botEntity.discard();
        }
    }
}
