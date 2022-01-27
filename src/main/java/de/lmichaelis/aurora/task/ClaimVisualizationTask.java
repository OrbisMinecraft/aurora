// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.task;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClaimVisualizationTask implements Runnable {
	private static final Particle PARTICLE = Particle.REDSTONE.builder().force(false).particle();
	private final @NotNull Player player;

	private final Particle.DustOptions options;
	private final Claim claim;
	private int id;

	public ClaimVisualizationTask(final @NotNull Claim claim, final @NotNull Player player, Color color) {
		this.player = player;
		this.claim = claim;
		this.options = new Particle.DustOptions(color, 3);
	}

	public static @NotNull ClaimVisualizationTask spawn(final @NotNull Claim claim, final @NotNull Player player, Color color, final int cancelAfterTicks) {
		final var task = new ClaimVisualizationTask(claim, player, color);
		task.id = Bukkit.getScheduler().scheduleSyncRepeatingTask(Aurora.instance, task, 0, 20);
		Bukkit.getScheduler().scheduleSyncDelayedTask(Aurora.instance, task::cancel, cancelAfterTicks);
		return task;
	}


	private void drawLine(double x1, double y1, double z1, double x2, double y2, double z2) {
		double distanceX = Math.abs(x2 - x1), distanceY = Math.abs(y2 - y1), distanceZ = Math.abs(z2 - z1);
		double stepCount = (Math.max(Math.max(distanceX, distanceY), distanceZ)) / 2;
		double stepSizeX = distanceX / stepCount, stepSizeY = distanceY / stepCount, stepSizeZ = distanceZ / stepCount;

		final var location = new Location(player.getWorld(), x1, y1, z1);

		for (int step = 0; step < stepCount + 1; step++) {
			if (location.distanceSquared(player.getLocation()) > 100 * 100) break;
			player.spawnParticle(PARTICLE, location, 1, 0, 0, 0, options);
			location.add(stepSizeX, stepSizeY, stepSizeZ);
		}
	}

	private void drawVerticalLine(double x, double z, double y1, double y2) {
		drawLine(x, y1, z, x, y2, z);
	}

	private void drawHorizontalLineX(double x, double y, double z1, double z2) {
		drawLine(x, y, z1, x, y, z2);
	}

	private void drawHorizontalLineZ(double y, double z, double x1, double x2) {
		drawLine(x1, y, z, x2, y, z);
	}

	@Override
	public void run() {
		drawVerticalLine(claim.minX, claim.minZ, claim.minY, claim.maxY);
		drawVerticalLine(claim.maxX + 1, claim.minZ, claim.minY, claim.maxY);
		drawVerticalLine(claim.minX, claim.maxZ + 1, claim.minY, claim.maxY);
		drawVerticalLine(claim.maxX + 1, claim.maxZ + 1, claim.minY, claim.maxY);

		drawHorizontalLineX(claim.minX, claim.minY, claim.minZ, claim.maxZ + 1);
		drawHorizontalLineX(claim.maxX + 1, claim.minY, claim.minZ, claim.maxZ + 1);
		drawHorizontalLineX(claim.minX, claim.maxY + 1, claim.minZ, claim.maxZ + 1);
		drawHorizontalLineX(claim.maxX + 1, claim.maxY + 1, claim.minZ, claim.maxZ + 1);

		drawHorizontalLineZ(claim.minY, claim.minZ, claim.minX, claim.maxX);
		drawHorizontalLineZ(claim.minY, claim.maxZ + 1, claim.minX, claim.maxX);
		drawHorizontalLineZ(claim.maxY + 1, claim.minZ, claim.minX, claim.maxX);
		drawHorizontalLineZ(claim.maxY + 1, claim.maxZ + 1, claim.minX, claim.maxX);
	}

	public void cancel() {
		if (id != -1) {
			Bukkit.getScheduler().cancelTask(id);
			id = -1;
		}
	}
}
