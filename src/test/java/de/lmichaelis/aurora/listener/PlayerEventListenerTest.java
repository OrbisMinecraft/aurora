// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.plugin.PluginManagerMock;
import de.lmichaelis.aurora.Aurora;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerEventListenerTest {
	private ServerMock server;
	private WorldMock world;
	private PluginManagerMock emitter;

	private Aurora plugin;
	private PlayerMock player;

	@BeforeEach
	void setUp() {
		server = MockBukkit.mock();
		world = server.addSimpleWorld("world");
		plugin = MockBukkit.load(Aurora.class);
		emitter = server.getPluginManager();

		player = server.addPlayer();
		player.setLocation(at(0, 0, 0));
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Contract(value = "_, _, _ -> new", pure = true)
	private @NotNull Location at(int x, int y, int z) {
		return new Location(world, x, y, z);
	}

	@Test
	void onPlayerInteract() {
		// Left clicks should be ignored
		var event = new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, null, null, BlockFace.SELF, EquipmentSlot.HAND);
		emitter.callEvent(event);
		assertEquals(Event.Result.DENY, event.useInteractedBlock());
		assertEquals(Event.Result.DEFAULT, event.useItemInHand());

		event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, new BlockMock(Material.STONE, player.getLocation()), BlockFace.UP, EquipmentSlot.HAND);
		emitter.callEvent(event);
		assertEquals(Event.Result.ALLOW, event.useInteractedBlock());
		assertEquals(Event.Result.DEFAULT, event.useItemInHand());
	}
}