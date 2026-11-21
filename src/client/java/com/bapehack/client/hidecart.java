package com.bapehack.client.hidecart;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

/**
 * HideCart - wszystko w jednym pliku: stan, komenda i blokady interakcji.
 *
 * Gdy wlaczony, wszystkie wagoniki (zwykly, chest, hopper, furnace, tnt, spawner, command block)
 * sa dla Ciebie "duchami": brak kolizji, nie da sie ich trafic, otworzyc ani wsiasc.
 *
 * Komendy:
 *   /bapehack hidecart        -> przelacza
 *   /bapehack hidecart on     -> wlacza
 *   /bapehack hidecart off    -> wylacza
 */
public final class HideCart {
    private static boolean enabled = false;

    private HideCart() {}

    public static boolean isEnabled() {
        return enabled;
    }

    /** Czy ten entity to wagonik, ktory teraz ignorujemy (tylko po stronie klienta). Uzywa tego mixin. */
    public static boolean shouldIgnore(Entity entity) {
        return enabled && entity instanceof AbstractMinecartEntity && entity.getWorld().isClient;
    }

    /** Wywolaj raz z BapeHackClient. */
    public static void init() {
        // dodatkowe zabezpieczenie: nawet jakby cos trafilo wagonik, nie pozwol z nim interagowac
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                shouldIgnore(entity) ? ActionResult.FAIL : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                shouldIgnore(entity) ? ActionResult.FAIL : ActionResult.PASS);

        // komenda /bapehack hidecart [on|off]
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("bapehack")
                        .then(ClientCommandManager.literal("hidecart")
                                .executes(ctx -> set(ctx.getSource(), !enabled))
                                .then(ClientCommandManager.literal("on")
                                        .executes(ctx -> set(ctx.getSource(), true)))
                                .then(ClientCommandManager.literal("off")
                                        .executes(ctx -> set(ctx.getSource(), false))))));
    }

    private static int set(FabricClientCommandSource source, boolean on) {
        enabled = on;
        source.sendFeedback(Text.literal("[BapeHack] HideCart: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED)));
        return 1;
    }
}
