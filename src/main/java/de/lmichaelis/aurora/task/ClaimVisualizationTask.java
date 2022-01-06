// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.task;

import de.lmichaelis.aurora.model.Claim;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClaimVisualizationTask implements Runnable {
	private final @NotNull Player player;

	private final Claim claim;

	private final int particleDensityX, particleDensityY, particleDensityZ;
	private final Particle.DustOptions options;


	public ClaimVisualizationTask(final @NotNull Claim claim, final @NotNull Player player, Color color) {
		this.player = player;
		this.claim = claim;
		this.options = new Particle.DustOptions(color, 2);

		this.particleDensityX = (claim.maxX - claim.minX) / 2;
		this.particleDensityY = (claim.maxY - claim.minY) / 2;
		this.particleDensityZ = (claim.maxZ - claim.minZ) / 2;
	}


	private void drawLineX(int x, int y, int z) {
		player.spawnParticle(Particle.REDSTONE, x, y, z, particleDensityX, particleDensityX / 2.f, 0, 0, options);
	}

	private void drawLineY(int x, int y, int z) {
		player.spawnParticle(Particle.REDSTONE, x, y, z, particleDensityY, 0, particleDensityY / 2.f, 0, options);
	}

	private void drawLineZ(int x, int y, int z) {
		player.spawnParticle(Particle.REDSTONE, x, y, z, particleDensityZ, 0, 0, particleDensityZ / 2.f, options);
	}

	@Override
	public void run() {
		// Draw the corners
		drawLineY(claim.minX, claim.minY + particleDensityY, claim.minZ);
		drawLineY(claim.minX, claim.minY + particleDensityY, claim.maxZ + 1);
		drawLineY(claim.maxX + 1, claim.minY + particleDensityY, claim.minZ);
		drawLineY(claim.maxX + 1, claim.minY + particleDensityY, claim.maxZ + 1);

		// Draw the bottom rectangle
		drawLineX(claim.minX + particleDensityX, claim.minY, claim.minZ);
		drawLineX(claim.minX + particleDensityX, claim.minY, claim.maxZ + 1);
		drawLineZ(claim.minX, claim.minY, claim.minZ + particleDensityZ);
		drawLineZ(claim.maxX + 1, claim.minY, claim.minZ + particleDensityZ);

		// Draw the top rectangle
		drawLineX(claim.minX + particleDensityX, claim.maxY, claim.minZ);
		drawLineX(claim.minX + particleDensityX, claim.maxY, claim.maxZ + 1);
		drawLineZ(claim.minX, claim.maxY, claim.minZ + particleDensityZ);
		drawLineZ(claim.maxX + 1, claim.maxY, claim.minZ + particleDensityZ);
	}
}
