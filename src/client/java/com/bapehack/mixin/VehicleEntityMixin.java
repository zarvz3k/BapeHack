package com.bapehack.mixin;

import com.bapehack.BapeHackClient;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleEntity.class)
public class VehicleEntityMixin {
    // wagon nie jest celem: bijesz, otwierasz skrzynki i piece "przez" niego
    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void bh$noMinecartHit(CallbackInfoReturnable<Boolean> cir) {
        if (BapeHackClient.noMinecartHit && (Object) this instanceof AbstractMinecartEntity) {
            cir.setReturnValue(false);
        }
    }
}
