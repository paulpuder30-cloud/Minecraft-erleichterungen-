package de.erleichterungen.werkzeuge.test;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Testwelt: 3x3-Spitzhacke muss 9 Blöcke abbauen, Hyperion muss 10 Blöcke teleportieren,
 * Zombies im Umkreis töten und einen Absorptions-Schild geben.
 */
public class WerkzeugeClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			var srv = sp.getServer();
			srv.runCommand("time set day");
			srv.runCommand("gamemode survival @a");
			srv.runCommand("difficulty easy");
			srv.runCommand("gamerule spawn_mobs false");
			BlockPos base = ctx.computeOnClient(mc -> mc.player.blockPosition()).above(10);
			int x = base.getX(), y = base.getY(), z = base.getZ();
			srv.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", x - 8, y - 1, z - 3, x + 8, y - 1, z + 20));
			srv.runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", x - 8, y, z - 3, x + 8, y + 6, z + 20));
			// Wand 3 Blöcke vor dem Spieler
			srv.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", x - 2, y, z + 2, x + 2, y + 3, z + 2));
			srv.runCommand(String.format("tp @a %d.5 %d %d.5 0 0", x, y, z));
			srv.runCommand("item replace entity @a hotbar.0 with werkzeuge:pickaxe_3x3[enchantments={efficiency:5}]");
			srv.runCommand("item replace entity @a hotbar.1 with werkzeuge:hyperion");
			ctx.waitTicks(20);
			TestInput in = ctx.getInput();

			// ---- 3x3-Spitzhacke: Blick nach Süden auf (x, y+1, z+2)
			srv.runCommand(String.format("tp @a %d.5 %d %d.5 0 12", x, y, z));
			ctx.waitTicks(5);
			BlockPos target = new BlockPos(x, y + 1, z + 2);
			for (int t = 0; t < 20 && !ctx.computeOnClient(mc -> mc.level.getBlockState(target).isAir()); t++) {
				ctx.runOnClient(mc -> {
					if (mc.gameMode.continueDestroyBlock(target, net.minecraft.core.Direction.NORTH)) {
						mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
					}
				});
				ctx.waitTick();
			}
			ctx.waitTicks(5);
			int left = ctx.computeOnClient(mc -> {
				int n = 0;
				for (BlockPos p : BlockPos.betweenClosed(new BlockPos(x - 1, y, z + 2), new BlockPos(x + 1, y + 2, z + 2)))
					if (!mc.level.getBlockState(p).isAir()) n++;
				return n;
			});
			boolean edgesIntact = ctx.computeOnClient(mc -> !mc.level.getBlockState(new BlockPos(x - 2, y + 1, z + 2)).isAir()
					&& !mc.level.getBlockState(new BlockPos(x, y + 3, z + 2)).isAir());
			ctx.takeScreenshot("01-3x3-spitzhacke");
			System.out.println("[Werkzeuge-Test] 3x3: übrig " + left + ", Rand intakt " + edgesIntact);
			if (left != 0) throw new AssertionError("3x3-Spitzhacke hat " + left + " von 9 Blöcken übrig gelassen");
			if (!edgesIntact) throw new AssertionError("3x3-Spitzhacke hat mehr als 3x3 abgebaut");

			// ---- Hyperion
			srv.runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", x - 2, y, z + 2, x + 2, y + 3, z + 2));
			srv.runCommand(String.format("tp @a %d.5 %d %d.5 0 0", x, y, z));
			for (int i = 0; i < 3; i++) {
				srv.runCommand(String.format("summon minecraft:zombie %d %d %d {NoAI:1b}", x - 3 + i * 3, y, z + 14));
			}
			srv.runCommand(String.format("summon minecraft:zombie %d %d %d {NoAI:1b}", x, y, z - 2)); // zu weit weg (12 Blöcke)
			in.pressKey(InputConstants.KEY_2);
			ctx.waitTicks(5);
			Vec3 before = ctx.computeOnClient(mc -> mc.player.position());
			ctx.runOnClient(mc -> mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND));
			ctx.waitTicks(10);
			Vec3 after = ctx.computeOnClient(mc -> mc.player.position());
			float absorption = ctx.computeOnClient(mc -> mc.player.getAbsorptionAmount());
			ctx.takeScreenshot("02-hyperion");
			int alive = ctx.computeOnClient(mc -> mc.level.getEntitiesOfClass(Zombie.class,
					new AABB(x - 10, y - 2, z + 10, x + 10, y + 5, z + 18), Zombie::isAlive).size());
			int farAlive = ctx.computeOnClient(mc -> mc.level.getEntitiesOfClass(Zombie.class,
					new AABB(x - 2, y - 2, z - 4, x + 2, y + 5, z), Zombie::isAlive).size());
			double dist = before.distanceTo(after);
			System.out.println("[Werkzeuge-Test] Hyperion: Teleport " + dist + " Blöcke, Zombies übrig " + alive
					+ ", entfernter Zombie lebt " + farAlive + ", Absorption " + absorption);
			if (Math.abs(dist - 10) > 0.3) throw new AssertionError("Teleport nicht 10 Blöcke: " + dist);
			if (alive != 0) throw new AssertionError("Implosion hat nicht alle Zombies getroffen: " + alive);
			if (farAlive != 1) throw new AssertionError("Implosion hat zu weit entfernten Zombie getroffen");
			if (absorption <= 0) throw new AssertionError("Kein Wither-Schild");

			// Teleport stoppt vor Wänden
			srv.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", x - 2, y, z + 14, x + 2, y + 3, z + 14));
			srv.runCommand(String.format("tp @a %d.5 %d %d.5 0 0", x, y, z + 10));
			ctx.waitTicks(5);
			ctx.runOnClient(mc -> mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND));
			ctx.waitTicks(10);
			double zAfter = ctx.computeOnClient(mc -> mc.player.getZ());
			System.out.println("[Werkzeuge-Test] Teleport gegen Wand: z = " + zAfter + " (Wand bei " + (z + 14) + ")");
			if (zAfter > z + 14 - 0.29) throw new AssertionError("Teleport ging in die Wand: " + zAfter);
			if (zAfter < z + 13) throw new AssertionError("Teleport zu kurz vor der Wand: " + zAfter);
		}
	}
}
