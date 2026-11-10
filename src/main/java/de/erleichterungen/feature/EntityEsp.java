package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Markiert Entities, die typisch für Basen sind (Item Frames, Rüstungsständer, Truhen-Loren, ...),
 * durch Wände hindurch. Typen und Farben sind frei konfigurierbar.
 */
public final class EntityEsp {
	/** EntityType -> ARGB-Farbe. */
	private static final Map<EntityType<?>, Integer> COLORS = new IdentityHashMap<>();
	/** Anzahl gefundener Entities je Typ im letzten Frame (für das HUD). */
	private static final Map<EntityType<?>, Integer> COUNTS = new IdentityHashMap<>();
	private static int lastRendered;

	private EntityEsp() {
	}

	public static void init() {
		rebuildFilter();
	}

	public static void rebuildFilter() {
		COLORS.clear();
		for (Map.Entry<String, String> e : ModConfig.get().entityEspTypes.entrySet()) {
			EntityType<?> type = resolve(e.getKey());
			if (type != null) {
				COLORS.put(type, ModConfig.parseColor(e.getValue(), BlockEsp.defaultColor(e.getKey())));
			}
		}
	}

	public static EntityType<?> resolve(String id) {
		Identifier ident = Identifier.tryParse(BlockEsp.normalize(id));
		if (ident == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(ident)) return null;
		return BuiltInRegistries.ENTITY_TYPE.getValue(ident);
	}

	public static int lastRendered() {
		return lastRendered;
	}

	/** Die häufigsten Typen im Umkreis, z.B. "Item Frame 5". */
	public static List<String> topCounts(int limit) {
		List<Map.Entry<EntityType<?>, Integer>> entries = new ArrayList<>(COUNTS.entrySet());
		entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
		List<String> out = new ArrayList<>();
		for (int i = 0; i < Math.min(limit, entries.size()); i++) {
			Map.Entry<EntityType<?>, Integer> e = entries.get(i);
			out.add(e.getKey().getDescription().getString() + " " + e.getValue());
		}
		if (entries.size() > limit) out.add("...");
		return out;
	}

	public static int colorOf(EntityType<?> type) {
		return COLORS.getOrDefault(type, 0xFFFFFFFF);
	}

	public static void render(Minecraft mc, Vec3 cam, float partialTick) {
		ModConfig cfg = ModConfig.get();
		lastRendered = 0;
		COUNTS.clear();
		if (!cfg.entityEspEnabled || mc.level == null || COLORS.isEmpty()) return;

		double rangeSq = (double) cfg.entityEspRange * cfg.entityEspRange;
		int max = cfg.entityEspMaxBoxes;

		for (Entity entity : mc.level.entitiesForRendering()) {
			if (lastRendered >= max) break;
			Integer color = COLORS.get(entity.getType());
			if (color == null || entity.isRemoved()) continue;
			Vec3 pos = entity.getPosition(partialTick);
			double distSq = pos.distanceToSqr(cam);
			if (distSq > rangeSq) continue;

			AABB box = entity.getBoundingBox().move(pos.subtract(entity.position()));
			// Sehr flache Boxen (Item Frames, Gemälde) minimal aufdicken, damit sie sichtbar bleiben
			box = box.inflate(0.02);
			GizmoStyle style = cfg.entityEspFill
					? GizmoStyle.strokeAndFill(color, cfg.entityEspLineWidth, (color & 0x00FFFFFF) | 0x30000000)
					: GizmoStyle.stroke(color, cfg.entityEspLineWidth);
			Gizmos.cuboid(box, style).setAlwaysOnTop();

			if (cfg.entityEspLabels) {
				String label = label(entity);
				if (label != null) {
					double dist = Math.sqrt(distSq);
					float scale = (float) Math.min(3.0, Math.max(0.5, dist / 14.0)) * 0.45f;
					Vec3 top = new Vec3((box.minX + box.maxX) / 2, box.maxY + 0.2 + scale * 0.3, (box.minZ + box.maxZ) / 2);
					Gizmos.billboardText(label + String.format(Locale.ROOT, " [%.0fm]", dist), top,
							TextGizmo.Style.forColorAndCentered(color | 0xFF000000).withScale(scale)).setAlwaysOnTop();
				}
			}
			COUNTS.merge(entity.getType(), 1, Integer::sum);
			lastRendered++;
		}
	}

	private static String label(Entity entity) {
		if (entity instanceof ItemFrame frame) {
			ItemStack item = frame.getItem();
			return item.isEmpty() ? entity.getType().getDescription().getString()
					: item.getHoverName().getString();
		}
		if (entity.hasCustomName() && entity.getCustomName() != null) {
			return entity.getCustomName().getString();
		}
		return entity.getType().getDescription().getString();
	}
}
