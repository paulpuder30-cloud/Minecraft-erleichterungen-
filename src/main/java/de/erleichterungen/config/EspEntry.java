package de.erleichterungen.config;

/** Ein Eintrag in der Block- bzw. Entity-ESP-Liste. */
public final class EspEntry {
	/** Box-Farbe (Hex RRGGBB). */
	public String color;
	/** Tracer-Farbe (Hex RRGGBB). */
	public String tracerColor;
	public RenderMode mode = RenderMode.BOX;

	public EspEntry() {
	}

	public EspEntry(String color, String tracerColor, RenderMode mode) {
		this.color = color;
		this.tracerColor = tracerColor;
		this.mode = mode;
	}

	public static EspEntry of(String color) {
		return new EspEntry(color, color, RenderMode.BOX);
	}

	void normalize() {
		if (color == null) color = "FFFFFF";
		if (tracerColor == null) tracerColor = color;
		if (mode == null) mode = RenderMode.BOX;
	}

	public RenderMode mode() {
		return mode == null ? RenderMode.BOX : mode;
	}

	public int boxColor() {
		return ModConfig.parseColor(color, 0xFFFFFFFF);
	}

	public int tracerColor() {
		return ModConfig.parseColor(tracerColor != null ? tracerColor : color, boxColor());
	}
}
