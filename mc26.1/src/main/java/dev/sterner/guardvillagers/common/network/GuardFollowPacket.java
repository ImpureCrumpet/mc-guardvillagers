package dev.sterner.guardvillagers.common.network;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;

public record GuardFollowPacket(int guardId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GuardFollowPacket> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "guard_follow"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuardFollowPacket> PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            GuardFollowPacket::guardId,
            GuardFollowPacket::new
    );

    public void handle(ServerPlayNetworking.Context context) {
        Entity entity = context.player().level().getEntity(guardId);
        if (entity instanceof GuardEntity guardEntity) {
            guardEntity.setFollowing(!guardEntity.isFollowing());
            guardEntity.setOwnerId(context.player().getUUID());
            guardEntity.playSound(SoundEvents.VILLAGER_YES, 1, 1);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
