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
import java.util.Objects;

public class AuroraSubdivideCommand extends AuroraBaseCommand {
	public static final BossBar SUBDIVIDE_MODE_BOSS_BAR = Bukkit.createBossBar(
			"§bAurora claim subdivision mode active",
			BarColor.BLUE,
			BarStyle.SOLID
	);

	public AuroraSubdivideCommand(Aurora plugin) {
		super(plugin);
	}

	@Override
	public boolean hasPermission(CommandSender sender) {
		return sender.hasPermission("aurora.claims");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof final Player player)) return false;
		final var user = Objects.requireNonNull(User.fromMetadata(player));

		if (user.subdivideMode) {
			user.subdivideMode = false;
			player.sendMessage(plugin.config.messages.leaveSubdivideMode);
			SUBDIVIDE_MODE_BOSS_BAR.removePlayer(player);
		} else {
			user.subdivideMode = true;
			player.sendMessage(plugin.config.messages.enterSubdivideMode);
			SUBDIVIDE_MODE_BOSS_BAR.addPlayer(player);
		}

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return List.of();
	}
}
