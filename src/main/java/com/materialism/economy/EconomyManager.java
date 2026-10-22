package com.materialism.economy;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public final class EconomyManager {
    private static final Map<MinecraftServer, EconomyStore> STORES = new HashMap<>();

    private EconomyManager() {
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            EconomyStore store;
            synchronized (STORES) {
                store = STORES.remove(server);
            }

            if (store != null) {
                store.save();
            }
        });
    }

    public static EconomyStore get(MinecraftServer server) {
        synchronized (STORES) {
            return STORES.computeIfAbsent(server, EconomyStore::open);
        }
    }
}
