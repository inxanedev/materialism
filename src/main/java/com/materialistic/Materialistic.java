package com.materialistic;

import com.materialistic.command.BalanceCommand;
import com.materialistic.command.SellCommand;
import com.materialistic.command.ShopCommand;
import com.materialistic.economy.EconomyManager;
import com.materialistic.menu.ModMenus;
import com.materialistic.network.MaterialisticNetworking;
import com.materialistic.network.ShopNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Materialistic implements ModInitializer {
    public static final String MOD_ID = "materialistic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModMenus.init();
        WorthData.get();
        EconomyManager.init();
        MaterialisticNetworking.init();
        ShopNetworking.init();
        SellCommand.register();
        BalanceCommand.register();
        ShopCommand.register();
        LOGGER.info("Materialistic loaded");
    }
}
