package dev.kara.uuidbridge.fabric;

import dev.kara.uuidbridge.UuidBridge;
import dev.kara.uuidbridge.command.UuidBridgeCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class UuidBridgeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        UuidBridge.init(FabricLoader.getInstance().getGameDir());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            UuidBridgeCommands.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTING.register(UuidBridge::runPendingMigration);
    }
}
