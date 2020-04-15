package net.earthcomputer.multiconnect.mixin;

import net.earthcomputer.multiconnect.protocols.AbstractProtocol;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemModelMesher.class)
public class MixinItemModels {

    @Inject(method = "getIndex", at = @At("HEAD"), cancellable = true)
    private static void getRawModelId(Item item, CallbackInfoReturnable<Integer> ci) {
        ci.setReturnValue(AbstractProtocol.getUnmodifiedId(Registry.ITEM, item));
    }

}
