package com.bapehack.mixin;

import com.bapehack.BapeHackClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void bh$hideMinecarts(E entity, Frustum frustum, double x, double y, double z,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (BapeHackClient.hideCartEnabled && BapeHackClient.hideMinecarts && entity instanceof AbstractMinecartEntity) {
            cir.setReturnValue(false);
        }
    }
}
