package com.jamieswhiteshirt.clotheslinefabric.common.network.message;

import com.jamieswhiteshirt.clotheslinefabric.common.util.PacketByteBufSerialization;
import net.minecraft.util.PacketByteBuf;

public class HitNetworkMessage {
    public final int networkId;
    public final int attachmentKey;
    public final int offset;

    public HitNetworkMessage(int networkId, int attachmentKey, int offset) {
        this.networkId = networkId;
        this.attachmentKey = attachmentKey;
        this.offset = offset;
    }

    public void serialize(PacketByteBuf buf) {
        PacketByteBufSerialization.writeNetworkId(buf, networkId);
        buf.writeInt(attachmentKey);
        buf.writeInt(offset);
    }

    public static HitNetworkMessage deserialize(PacketByteBuf buf) {
        return new HitNetworkMessage(
            PacketByteBufSerialization.readNetworkId(buf),
            buf.readInt(),
            buf.readInt()
        );
    }
}
