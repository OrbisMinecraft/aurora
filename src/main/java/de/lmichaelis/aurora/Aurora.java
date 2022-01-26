// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora;

import de.lmichaelis.aurora.command.*;
import de.lmichaelis.aurora.config.AuroraConfig;
import de.lmichaelis.aurora.listener.*;
import de.lmichaelis.aurora.task.AccrueClaimBlocksTask;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Main class for the Aurora plugin which is instantiated and loaded by the
 * bukkit server.
 */
public final class Aurora extends JavaPlugin {
	public static Logger logger;
	public static Database db;
	public static Aurora instance;
	public AuroraConfig config;

	private BaseListener[] listeners;
	private AuroraRootCommand command;
	private Integer accrueClaimBlocksTaskId = null;

	public Aurora() {
		super();
	}

	Aurora(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
		super(loader, description, dataFolder, file);
	}

	/**
	 * Initializes the plugin by loading its configuration, connecting to a
	 * database and loading all properties needed for startup.
	 */
	@Override
	public void onEnable() {
		Aurora.instance = this;

		// Register the base command
		this.command = new AuroraRootCommand(this);
		this.command.addSubCommand("reload", new AuroraReloadCommand(this));
		this.command.addSubCommand("unclaim", new AuroraUnclaimCommand(this));
		this.command.addSubCommand("claimlist", new AuroraClaimListCommand(this));
		this.command.addSubCommand("set-group", new AuroraSetGroupCommand(this));
		this.command.addSubCommand("admin", new AuroraAdminModeCommand(this));
		this.command.addSubCommand("setting", new AuroraClaimSettingsCommand(this));

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
		Aurora.logger = this.getLogger();

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

		// Start the task to add claim blocks to every online player every 5 minutes
		if (accrueClaimBlocksTaskId != null) this.getServer().getScheduler().cancelTask(this.accrueClaimBlocksTaskId);
		if (config.accrueClaimBlockEnabled && config.accrueClaimBlocksPerHour > 0) {
			accrueClaimBlocksTaskId = this.getServer().getScheduler().scheduleSyncRepeatingTask(
					this,
					new AccrueClaimBlocksTask(config.accrueClaimBlocksPerHour, config.accrueClaimBlocksLimit),
					20 * 60 * 5,
					20 * 60 * 5
			);
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
			getLogger().severe("Failed to properly unload: %s".formatted(e));
		}
	}
}
