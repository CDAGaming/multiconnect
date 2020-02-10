package net.earthcomputer.multiconnect.protocols.v1_15_2;

import net.earthcomputer.multiconnect.protocols.ProtocolRegistry;
import net.earthcomputer.multiconnect.protocols.V1_15_combat_6.Protocol_1_15_combat_6;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.ChatVisibility;
import net.minecraft.network.Packet;
import net.minecraft.server.network.packet.ClientSettingsC2SPacket;
import net.minecraft.server.network.packet.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class Protocol_1_15_2 extends Protocol_1_15_combat_6 {
    public static void registerTranslators() {
        ProtocolRegistry.registerOutboundTranslator(ClientSettingsC2SPacket.class, buf -> {
            buf.passthroughWrite(String.class); // language
            buf.passthroughWrite(Byte.class); // viewDistance
            buf.passthroughWrite(ChatVisibility.class); // chatVisibility
            buf.passthroughWrite(Boolean.class); // chatColors
            buf.passthroughWrite(Byte.class); // playerModelBitMask
            buf.passthroughWrite(Arm.class); // mainArm
            buf.skipWrite(Boolean.class); // method_24343 - FIXME
        });
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (packet.getClass() == PlayerInteractEntityC2SPacket.class) {
            if (((PlayerInteractEntityC2SPacket) packet).getType() == PlayerInteractEntityC2SPacket.InteractionType.AIR_SWING) {
                if (MinecraftClient.getInstance().crosshairTarget != null && MinecraftClient.getInstance().interactionManager != null && MinecraftClient.getInstance().crosshairTarget.getType() == HitResult.Type.ENTITY) {
                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult)MinecraftClient.getInstance().crosshairTarget).getEntity());
                }
                return false;
            }
        }
        return super.onSendPacket(packet);
    }
}
