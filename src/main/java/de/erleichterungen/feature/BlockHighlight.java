package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Block-Highlight: hebt den Block hervor, auf den der Spieler gerade schaut.
 */
public final class BlockHighlight {
	private BlockHighlight() {
	}

	public static void render(Minecraft mc, Vec3 cam, float partialTick) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.blockHighlightEnabled || mc.level == null) return;

		HitResult hit = mc.hitResult;
		if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) return;

		AABB box = new AABB(bhr.getBlockPos()).inflate(0.002);
		int color = ModConfig.parseColor(cfg.blockHighlightColor, 0xFFFFFFFF);
		GizmoStyle style = cfg.blockHighlightFill
				? GizmoStyle.strokeAndFill(color, cfg.blockHighlightLineWidth, (color & 0x00FFFFFF) | 0x33000000)
				: GizmoStyle.stroke(color, cfg.blockHighlightLineWidth);
		var gizmo = Gizmos.cuboid(box, style);
		if (cfg.blockHighlightThroughWalls) gizmo.setAlwaysOnTop();
	}
}
