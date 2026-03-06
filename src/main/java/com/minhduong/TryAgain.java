package com.minhduong;

import com.minhduong.data.HomeManager;
import com.minhduong.data.PlayerDataManager;
import net.fabricmc.api.ModInitializer;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TryAgain implements ModInitializer {
	public static final String MOD_ID = "try-again";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static MinecraftServer server;

	@Override
	public void onInitialize() {
		PlayerDataManager.init();
		HomeManager.init();

	}
}