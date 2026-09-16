package com.ssaml.advancedoredeposits.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class AdvancedOreDepositsClient {
    private AdvancedOreDepositsClient() {
    }

    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, new IConfigScreenFactory() {
            @Override
            public Screen createScreen(ModContainer modContainer, Screen modListScreen) {
                return new ConfigurationScreen(modContainer, modListScreen);
            }
        });
    }
}
