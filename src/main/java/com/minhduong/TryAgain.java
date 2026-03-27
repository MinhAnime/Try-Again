package com.minhduong;

import com.minhduong.command.*;
import com.minhduong.data.HomeManager;
import com.minhduong.data.LanguageManager;
import com.minhduong.data.PlayerDataManager;
import com.minhduong.events.AuthEventHandler;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TryAgain implements ModInitializer {
	public static final String MOD_ID = "try-again";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static MinecraftServer server;

	@Override
	public void onInitialize() {
		LOGGER.info("Loading...");
		LanguageManager.init();
		PlayerDataManager.init();
		HomeManager.init();


		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LoginCommand.register(dispatcher);
			RegisterCommand.register(dispatcher);
			HomeCommand.register(dispatcher);
			TpaCommand.register(dispatcher);
			HelpCommand.register(dispatcher);
			
			dispatcher.register(net.minecraft.server.command.CommandManager.literal("tryagain")
				.then(net.minecraft.server.command.CommandManager.literal("reload")
					.requires(source -> true)
					.executes(ctx -> {
						LanguageManager.load();
						ctx.getSource().sendMessage(com.minhduong.util.Messages.success("reloaded"));
						return 1;
					})
				)
			);
		});

		ServerPlayConnectionEvents.JOIN.register(AuthEventHandler::onPlayerJoin);
		ServerPlayConnectionEvents.DISCONNECT.register(AuthEventHandler::onPlayerLeave);
		ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
		LOGGER.info("Ready.");
	}
	public static MinecraftServer getServer() {
		return server;
	}
}