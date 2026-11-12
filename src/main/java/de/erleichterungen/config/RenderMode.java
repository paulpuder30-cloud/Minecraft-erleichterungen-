package de.erleichterungen.config;

import java.util.Locale;

/** Wie ein markiertes Ziel dargestellt wird. */
public enum RenderMode {
	BOX("Box"),
	BOTH("Box & Tracer"),
	TRACER("Nur Tracer");

	public final String label;

	RenderMode(String label) {
		this.label = label;
	}

	public boolean box() {
		return this != TRACER;
	}

	public boolean tracer() {
		return this != BOX;
	}

	public RenderMode next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public RenderMode prev() {
		return values()[(ordinal() + values().length - 1) % values().length];
	}

	/** Mittellange Form für Knöpfe. */
	public String buttonLabel() {
		return switch (this) {
			case BOX -> "Box";
			case BOTH -> "Box+Tracer";
			case TRACER -> "Tracer";
		};
	}

	/** Kurzform für Listen: "B", "B+T", "T". */
	public String shortLabel() {
		return switch (this) {
			case BOX -> "B";
			case BOTH -> "B+T";
			case TRACER -> "T";
		};
	}

	public static RenderMode parse(String s) {
		if (s == null) return null;
		return switch (s.toLowerCase(Locale.ROOT)) {
			case "box", "boxen", "b" -> BOX;
			case "both", "beide", "box&tracer", "boxtracer", "bt", "b+t" -> BOTH;
			case "tracer", "tracers", "t", "nurtracer" -> TRACER;
			default -> null;
		};
	}
}
