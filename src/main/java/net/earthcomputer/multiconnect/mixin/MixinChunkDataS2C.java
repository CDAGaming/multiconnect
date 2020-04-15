package net.earthcomputer.multiconnect.mixin;

import net.earthcomputer.multiconnect.transformer.ChunkData;
import net.earthcomputer.multiconnect.transformer.TransformerByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SChunkDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SChunkDataPacket.class)
public class MixinChunkDataS2C {

    @Inject(method = "getReadBuffer", at = @At("RETURN"), cancellable = true)
    private void onGetReadBuffer(CallbackInfoReturnable<PacketBuffer> ci) {
        TransformerByteBuf transformerByteBuf = new TransformerByteBuf(ci.getReturnValue(), null);
        transformerByteBuf.readTopLevelType(ChunkData.class);
        ci.setReturnValue(transformerByteBuf);
    }

}
