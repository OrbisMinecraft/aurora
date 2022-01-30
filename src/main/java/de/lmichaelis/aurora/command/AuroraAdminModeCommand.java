// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.command;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.User;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AuroraAdminModeCommand extends AuroraBaseCommand {
	public static final BossBar ADMIN_MODE_BOSS_BAR = Bukkit.createBossBar(
			"§cAurora admin mode active",
			BarColor.RED,
			BarStyle.SOLID
	);

	public AuroraAdminModeCommand(Aurora plugin) {
		super(plugin);
	}

	@Override
	public boolean hasPermission(CommandSender sender) {
		return sender.hasPermission("aurora.admin");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof final Player player)) return false;
		final var user = User.fromMetadata(player);
		assert user != null;

		if (user.adminMode) {
			user.adminMode = false;
			player.sendMessage(plugin.config.messages.leaveAdminMode);
			ADMIN_MODE_BOSS_BAR.removePlayer(player);
		} else {
			user.adminMode = true;
			player.sendMessage(plugin.config.messages.enterAdminMode);
			ADMIN_MODE_BOSS_BAR.addPlayer(player);
		}

		// Reset the current selection
		user.currentInteraction = null;
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return List.of();
	}
}
