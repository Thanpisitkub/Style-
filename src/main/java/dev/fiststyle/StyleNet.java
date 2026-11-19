package dev.fiststyle;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class StyleNet {
    private StyleNet() {}

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FistStyleMod.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, AutoPacket.class, AutoPacket::encode, AutoPacket::decode, AutoPacket::handle);
    }

    /** Client -> server: current state of the Auto Parry / Auto Dodge toggles. */
    public static void sendAuto(boolean parry, boolean dodge) {
        CHANNEL.sendToServer(new AutoPacket(parry, dodge));
    }

    public static final class AutoPacket {
        private final boolean parry;
        private final boolean dodge;

        public AutoPacket(boolean parry, boolean dodge) {
            this.parry = parry;
            this.dodge = dodge;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(this.parry);
            buf.writeBoolean(this.dodge);
        }

        public static AutoPacket decode(FriendlyByteBuf buf) {
            return new AutoPacket(buf.readBoolean(), buf.readBoolean());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    AutoCombat.setState(player, this.parry, this.dodge);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
