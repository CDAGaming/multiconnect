package net.earthcomputer.multiconnect.protocols.v1_13_2.mixin;

import net.earthcomputer.multiconnect.impl.MixinHelper;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.network.datasync.DataParameter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractArrowEntity.class)
public interface ProjectileEntityAccessor {
    @Accessor("PIERCE_LEVEL")
    static DataParameter<Byte> getPierceLevel() {
        return MixinHelper.fakeInstance();
    }
}
