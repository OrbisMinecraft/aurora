// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.command;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AuroraClaimSettingsCommand extends AuroraBaseCommand {
	public static final List<String> VARIABLES = List.of(
			"pvp",
			"mobGriefing",
			"explosions"
	);

	public AuroraClaimSettingsCommand(Aurora plugin) {
		super(plugin);
	}

	@Override
	public boolean hasPermission(CommandSender sender) {
		return sender.hasPermission("aurora.claims");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof final Player player)) return false;
		if (args.length < 3) return false;
		final var claim = Claim.getClaim(player.getLocation());

		if (claim == null) {
			player.sendMessage(plugin.config.messages.notAClaim);
		} else if (!claim.isAllowed(player, Group.MANAGE)) {
			player.sendMessage(plugin.config.messages.notClaimOwner);
		} else {
			final var enabled = args[2].equals("on");

			switch (args[1]) {
				case "pvp" -> {
					claim.pvpEnabled = enabled;
					player.sendMessage((enabled ? "Enabled" : "Disabled") + " PvP in this claim.");
				}
				case "mobGriefing" -> {
					claim.mobGriefing = enabled;
					player.sendMessage((enabled ? "Enabled" : "Disabled") + " mob griefing in this claim.");
				}
				case "explosions" -> {
					claim.allowsExplosions = enabled;
					player.sendMessage((enabled ? "Enabled" : "Disabled") + " explosions in this claim.");
				}
				default -> {
					return false;
				}
			}

			claim.update();
		}

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return args.length < 3 ? VARIABLES : List.of("on", "off");
	}
}
