package dev.kara.uuidbridge.forge;

import dev.kara.uuidbridge.UuidBridge;
import dev.kara.uuidbridge.command.UuidBridgeCommands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.common.Mod;

@Mod(UuidBridge.MOD_ID)
public final class UuidBridgeForge {
    public UuidBridgeForge() {
        UuidBridge.init(FMLPaths.GAMEDIR.get());
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerAboutToStart);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        UuidBridgeCommands.register(event.getDispatcher());
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        UuidBridge.runPendingMigration(event.getServer());
    }
}
