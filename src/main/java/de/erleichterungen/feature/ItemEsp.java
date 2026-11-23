package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import de.erleichterungen.util.Tracers;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Item-ESP: markiert am Boden liegende Gegenstände (ItemEntity) mit Box, Name und Anzahl –
 * durch Wände hindurch.
 */
public final class ItemEsp {
	private static int lastRendered;

	private ItemEsp() {
	}

	public static int lastRendered() {
		return lastRendered;
	}

	public static void render(Minecraft mc, Vec3 cam, float partialTick) {
		ModConfig cfg = ModConfig.get();
		lastRendered = 0;
		if (!cfg.itemEspEnabled || mc.level == null) return;

		double rangeSq = (double) cfg.itemEspRange * cfg.itemEspRange;
		int color = ModConfig.parseColor(cfg.itemEspColor, 0xFF55FFFF);
		int tracerColor = ModConfig.parseColor(cfg.itemEspTracerColor, 0xFF55FFFF);

		for (Entity entity : mc.level.entitiesForRendering()) {
			if (lastRendered >= cfg.itemEspMaxBoxes) break;
			if (!(entity instanceof ItemEntity item) || entity.isRemoved()) continue;
			Vec3 pos = entity.getPosition(partialTick);
			double distSq = pos.distanceToSqr(cam);
			if (distSq > rangeSq) continue;

			AABB box = entity.getBoundingBox().move(pos.subtract(entity.position())).inflate(0.05);
			if (cfg.itemEspMode.box()) {
				GizmoStyle style = cfg.itemEspFill
						? GizmoStyle.strokeAndFill(color, cfg.itemEspLineWidth, (color & 0x00FFFFFF) | 0x30000000)
						: GizmoStyle.stroke(color, cfg.itemEspLineWidth);
				Gizmos.cuboid(box, style).setAlwaysOnTop();
			}
			if (cfg.itemEspMode.tracer()) Tracers.line(box.getCenter(), tracerColor);

			if (cfg.itemEspLabels) {
				ItemStack stack = item.getItem();
				double dist = Math.sqrt(distSq);
				String label = stack.getHoverName().getString()
						+ (stack.getCount() > 1 ? " x" + stack.getCount() : "")
						+ String.format(Locale.ROOT, " [%.0fm]", dist);
				float scale = (float) Math.min(3.0, Math.max(0.5, dist / 14.0)) * 0.45f;
				Vec3 center = box.getCenter();
				Vec3 top = new Vec3(center.x, box.maxY + 0.2 + scale * 0.3, center.z);
				Gizmos.billboardText(label, top,
						TextGizmo.Style.forColorAndCentered(color | 0xFF000000).withScale(scale)).setAlwaysOnTop();
			}
			lastRendered++;
		}
	}
}
