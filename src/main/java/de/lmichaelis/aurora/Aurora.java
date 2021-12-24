// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora;

import de.lmichaelis.aurora.command.*;
import de.lmichaelis.aurora.config.AuroraConfig;
import de.lmichaelis.aurora.listener.*;
import org.apache.logging.log4j.Logger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Main class for the Aurora plugin which is instantiated and loaded by the
 * bukkit server.
 */
public final class Aurora extends JavaPlugin {
	public static Logger logger;
	public static Database db;
	public AuroraConfig config;

	private BaseListener[] listeners;
	private AuroraRootCommand command;

	/**
	 * Initializes the plugin by loading its configuration, connecting to a
	 * database and loading all properties needed for startup.
	 */
	@Override
	public void onEnable() {
		// Register the base command
		this.command = new AuroraRootCommand(this);
		this.command.addSubCommand("reload", new AuroraReloadCommand(this));

		final var rootCommand = this.getCommand("aurora");
		assert rootCommand != null;

		rootCommand.setTabCompleter(this.command);
		rootCommand.setExecutor(this.command);

		// Initialize Aurora
		this.onReload();
	}

	/**
	 * Reloads the plugin fully by re-reading the configuration file and reconnecting
	 * to the database.
	 */
	public void onReload() {
		Aurora.logger = this.getLog4JLogger();

		try {
			this.saveDefaultConfig();

			final var configFile = new File(this.getDataFolder(), "config.yml");
			this.config = AuroraConfig.load(configFile);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Failed to load configuration", e);
		}

		try {
			if (Aurora.db != null) Aurora.db.onDisable();
			Aurora.db = new Database(config.databaseUri);
		} catch (SQLException | IOException e) {
			throw new IllegalStateException("Failed to connect to the database", e);
		}

		if (listeners == null) {
			listeners = new BaseListener[]{
					new BlockEventListener(this),
					new EntityEventListener(this),
					new HangingEventListener(this),
					new PlayerEventListener(this),
					new RaidEventListener(this),
					new VehicleEventListener(this),
					new WorldEventListener(this),
			};

			final var pm = getServer().getPluginManager();
			for (final var listener : listeners) {
				pm.registerEvents(listener, this);
			}
		}
	}

	/**
	 * Disables all the plugin's features and saves the plugin's current state.
	 */
	@Override
	public void onDisable() {
		try {
			if (Aurora.db != null) Aurora.db.onDisable();
		} catch (IOException e) {
			getLog4JLogger().error("Failed to properly unload", e);
		}
	}
}
