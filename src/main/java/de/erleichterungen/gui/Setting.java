package de.erleichterungen.gui;

import java.util.Collection;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Eine Einstellung eines Moduls. Liest/schreibt direkt die Felder der {@link de.erleichterungen.config.ModConfig}.
 */
public abstract sealed class Setting permits Setting.Bool, Setting.Int, Setting.Dec, Setting.Color, Setting.Text,
		Setting.ListEdit, Setting.Action {
	public final String name;
	public final String description;
	/** Wird nach jeder Änderung aufgerufen (z.B. um Filter neu aufzubauen). */
	public Runnable onChange = () -> {
	};
	/** Einstellung nur anzeigen, wenn diese Bedingung erfüllt ist. */
	public BooleanSupplier visible = () -> true;

	protected Setting(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public Setting onChange(Runnable r) {
		this.onChange = r;
		return this;
	}

	public Setting visibleIf(BooleanSupplier condition) {
		this.visible = condition;
		return this;
	}

	public static final class Bool extends Setting {
		public final Supplier<Boolean> get;
		public final Consumer<Boolean> set;

		public Bool(String name, String description, Supplier<Boolean> get, Consumer<Boolean> set) {
			super(name, description);
			this.get = get;
			this.set = set;
		}
	}

	public static final class Int extends Setting {
		public final Supplier<Integer> get;
		public final Consumer<Integer> set;
		public final int min, max;

		public Int(String name, String description, int min, int max, Supplier<Integer> get, Consumer<Integer> set) {
			super(name, description);
			this.min = min;
			this.max = max;
			this.get = get;
			this.set = set;
		}
	}

	public static final class Dec extends Setting {
		public final Supplier<Float> get;
		public final Consumer<Float> set;
		public final float min, max, step;

		public Dec(String name, String description, float min, float max, float step, Supplier<Float> get, Consumer<Float> set) {
			super(name, description);
			this.min = min;
			this.max = max;
			this.step = step;
			this.get = get;
			this.set = set;
		}
	}

	/** Farbe als Hex-String (RRGGBB). */
	public static final class Color extends Setting {
		public final Supplier<String> get;
		public final Consumer<String> set;

		public Color(String name, String description, Supplier<String> get, Consumer<String> set) {
			super(name, description);
			this.get = get;
			this.set = set;
		}
	}

	public static final class Text extends Setting {
		public final Supplier<String> get;
		public final Consumer<String> set;

		public Text(String name, String description, Supplier<String> get, Consumer<String> set) {
			super(name, description);
			this.get = get;
			this.set = set;
		}
	}

	/** Liste von IDs mit Farbe (Blöcke / Entities), bearbeitet in einem eigenen Fenster. */
	public static final class ListEdit extends Setting {
		public final Supplier<Map<String, String>> map;
		/** Alle wählbaren IDs (z.B. Block-Registry). */
		public final Supplier<Collection<String>> options;

		public ListEdit(String name, String description, Supplier<Map<String, String>> map, Supplier<Collection<String>> options) {
			super(name, description);
			this.map = map;
			this.options = options;
		}
	}

	public static final class Action extends Setting {
		public final Runnable action;

		public Action(String name, String description, Runnable action) {
			super(name, description);
			this.action = action;
		}
	}
}
