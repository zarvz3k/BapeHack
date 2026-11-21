package com.bapehack;

import com.bapehack.hidecart.HideCart;
import net.fabricmc.api.ClientModInitializer;

public class BapeHackClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HideCart.init();
    }
}
