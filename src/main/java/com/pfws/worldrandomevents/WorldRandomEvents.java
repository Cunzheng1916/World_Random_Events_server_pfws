package com.pfws.worldrandomevents;

import com.pfws.worldrandomevents.command.EventCommands;
import com.pfws.worldrandomevents.config.ModConfig;
import com.pfws.worldrandomevents.event.EventInteractionHandler;
import com.pfws.worldrandomevents.event.EventManager;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldRandomEvents implements ModInitializer {
    public static final String MOD_ID = "world-random-events";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ModConfig CONFIG;
    public static EventManager EVENT_MANAGER;

    @Override
    public void onInitialize() {
        LOGGER.info("[WorldRandomEvents] Initializing World Random Events mod...");

        CONFIG = ModConfig.load();
        LOGGER.info("[WorldRandomEvents] Configuration loaded.");

        NetworkHandler.register();
        LOGGER.info("[WorldRandomEvents] Network packets registered.");

        EVENT_MANAGER = new EventManager();
        EVENT_MANAGER.registerAllEvents();
        LOGGER.info("[WorldRandomEvents] Event manager initialized with {} events.", EVENT_MANAGER.getEventCount());

        EventInteractionHandler.register();
        LOGGER.info("[WorldRandomEvents] Interaction handlers registered.");

        EventCommands.register();
        LOGGER.info("[WorldRandomEvents] Commands registered.");

        LOGGER.info("[WorldRandomEvents] Initialization complete!");
    }
}