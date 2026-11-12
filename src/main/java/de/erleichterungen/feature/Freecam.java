package de.erleichterungen.feature;

import de.erleichterungen.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam: Die Kamera löst sich vom Spieler und fliegt frei durch die Welt.
 * Der Spieler bleibt stehen (Bewegungs-Eingaben werden abgefangen), Mausbewegungen drehen die Kamera.
 * Umgesetzt über Mixins in Camera, KeyboardInput und Entity.turn.
 */
public final class Freecam {
	private static boolean active;
	private static Vec3 pos = Vec3.ZERO;
	private static Vec3 prevPos = Vec3.ZERO;
	private static float yaw;
	private static float pitch;

	private Freecam() {
	}

	public static boolean isActive() {
		return active;
	}

	public static void toggle() {
		if (active) disable();
		else enable();
	}

	public static void enable() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		Vec3 eye = mc.player.getEyePosition(1.0f);
		pos = eye;
		prevPos = eye;
		yaw = mc.player.getYRot();
		pitch = mc.player.getXRot();
		active = true;
	}

	public static void disable() {
		active = false;
	}

	/** Mausbewegung → Kamera drehen (gleiche Formel wie Entity.turn). */
	public static void turn(double dx, double dy) {
		yaw += (float) dx * 0.15f;
		pitch = Mth.clamp(pitch + (float) dy * 0.15f, -90.0f, 90.0f);
	}

	public static void tick(Minecraft mc) {
		if (!active) return;
		if (mc.player == null || mc.level == null || !mc.player.isAlive()) {
			disable();
			return;
		}
		prevPos = pos;
		if (mc.gui.screen() != null) return; // in Menüs nicht bewegen

		Options o = mc.options;
		double forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
		double strafe = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
		double vertical = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);

		double speed = ModConfig.get().freecamSpeed * (o.keySprint.isDown() ? 2.5 : 1.0);
		double rad = Math.toRadians(yaw);
		double sin = Math.sin(rad), cos = Math.cos(rad);
		// Minecraft: Blickrichtung bei Yaw 0 = +Z
		double mx = -sin * forward + cos * strafe;
		double mz = cos * forward + sin * strafe;
		Vec3 motion = new Vec3(mx, vertical, mz);
		if (motion.lengthSqr() > 1.0E-6) {
			motion = motion.normalize().scale(speed);
			pos = pos.add(motion);
		}
	}

	public static Vec3 position(float partialTick) {
		return prevPos.lerp(pos, partialTick);
	}

	public static float yaw() {
		return yaw;
	}

	public static float pitch() {
		return pitch;
	}
}
