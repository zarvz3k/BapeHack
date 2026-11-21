package com.bapehack.mixin.client;

import com.bapehack.client.hidecart.HideCart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin {

    /**
     * Wagonik nie jest "trafialny": celownik/raycast go pomija, wiec nie otworzysz go,
     * nie wsiadziesz, nie uderzysz - a to co jest ZA nim (np. villager) dostaje klik/uderzenie.
     */
    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void bapehack$canHit(CallbackInfoReturnable<Boolean> cir) {
        if (HideCart.shouldIgnore((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    /** Wagonik nie popycha graczy / innych entity. */
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true, require = 0)
    private void bapehack$isPushable(CallbackInfoReturnable<Boolean> cir) {
        if (HideCart.shouldIgnore((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    /** Wagonik nie koliduje z innymi entity. */
    @Inject(method = "collidesWith", at = @At("HEAD"), cancellable = true, require = 0)
    private void bapehack$collidesWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (HideCart.shouldIgnore((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
