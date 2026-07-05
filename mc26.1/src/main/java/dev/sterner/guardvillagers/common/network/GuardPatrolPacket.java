package dev.sterner.guardvillagers.common.network;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

public record GuardPatrolPacket(int guardId, boolean pressed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GuardPatrolPacket> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "guard_patrol"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuardPatrolPacket> PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, GuardPatrolPacket::guardId,
            ByteBufCodecs.BOOL, GuardPatrolPacket::pressed,
            GuardPatrolPacket::new
    );

    public void handle(ServerPlayNetworking.Context context) {

        Entity entity = context.player().level().getEntity(guardId);
        if (entity instanceof GuardEntity guardEntity) {
            BlockPos pos = guardEntity.blockPosition();
            if (guardEntity.blockPosition() != null) {
                guardEntity.setPatrolPos(pos);
            }
            guardEntity.setPatrolling(pressed);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}