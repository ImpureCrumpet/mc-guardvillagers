package dev.sterner.guardvillagers.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GuardData(int guardId) {
    public static final StreamCodec<RegistryFriendlyByteBuf, GuardData> PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            GuardData::guardId,
            GuardData::new
    );
}
