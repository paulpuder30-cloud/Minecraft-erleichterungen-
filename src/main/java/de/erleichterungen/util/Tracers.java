package de.erleichterungen.util;

import de.erleichterungen.config.ModConfig;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

/**
 * Tracer-Linien: vom Fadenkreuz (kurz vor der Kamera) zum Ziel, immer sichtbar.
 */
public final class Tracers {
	private static Vec3 start = Vec3.ZERO;

	private Tracers() {
	}

	/** Pro Frame vor dem Zeichnen aufrufen. */
	public static void setup(Vec3 cameraPos, float xRot, float yRot) {
		// Startpunkt etwas vor der Kamera, damit die Linie aus der Bildmitte kommt
		start = cameraPos.add(Vec3.directionFromRotation(xRot, yRot).scale(0.6));
	}

	public static void line(Vec3 target, int color) {
		Gizmos.line(start, target, color, ModConfig.get().tracerWidth).setAlwaysOnTop();
	}
}
