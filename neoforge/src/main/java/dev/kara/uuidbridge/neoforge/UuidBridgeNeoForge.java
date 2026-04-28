package dev.kara.uuidbridge.neoforge;

import dev.kara.uuidbridge.UuidBridge;
import dev.kara.uuidbridge.command.UuidBridgeCommands;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@Mod(UuidBridge.MOD_ID)
public final class UuidBridgeNeoForge {
    public UuidBridgeNeoForge() {
        UuidBridge.init(FMLPaths.GAMEDIR.get());
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        UuidBridgeCommands.register(event.getDispatcher());
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        UuidBridge.runPendingMigration(event.getServer());
    }
}
