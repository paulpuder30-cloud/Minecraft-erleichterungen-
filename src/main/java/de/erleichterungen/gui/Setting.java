package de.erleichterungen.gui;

import de.erleichterungen.config.EspEntry;
import de.erleichterungen.config.RenderMode;

import java.util.Collection;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Eine Einstellung eines Moduls. Liest/schreibt direkt die Felder der {@link de.erleichterungen.config.ModConfig}.
 */
public abstract sealed class Setting permits Setting.Bool, Setting.Int, Setting.Dec, Setting.Color, Setting.Text,
		Setting.Mode, Setting.Choice, Setting.ListEdit, Setting.Action {
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

	/** Darstellungs-Modus: Box / Box & Tracer / Nur Tracer (Klick wechselt). */
	public static final class Mode extends Setting {
		public final Supplier<RenderMode> get;
		public final Consumer<RenderMode> set;

		public Mode(String name, String description, Supplier<RenderMode> get, Consumer<RenderMode> set) {
			super(name, description);
			this.get = get;
			this.set = set;
		}
	}

	/** Auswahl aus festen Optionen (Klick wechselt, Rechtsklick zurück). */
	public static final class Choice extends Setting {
		public final String[] labels;
		public final java.util.function.IntSupplier get;
		public final java.util.function.IntConsumer set;

		public Choice(String name, String description, String[] labels, java.util.function.IntSupplier get,
				java.util.function.IntConsumer set) {
			super(name, description);
			this.labels = labels;
			this.get = get;
			this.set = set;
		}

		public int index() {
			int i = get.getAsInt();
			return i < 0 || i >= labels.length ? 0 : i;
		}

		public String label() {
			return labels[index()];
		}

		public void cycle(int dir) {
			set.accept(Math.floorMod(index() + dir, labels.length));
		}
	}

	/** Liste von IDs mit Farbe (Blöcke / Entities), bearbeitet in einem eigenen Fenster. */
	public static final class ListEdit extends Setting {
		public final Supplier<Map<String, EspEntry>> map;
		/** Alle wählbaren IDs (z.B. Block-Registry). */
		public final Supplier<Collection<String>> options;
		/** ID -> lesbarer (übersetzter) Name, z.B. "minecraft:diamond_ore" -> "Diamanterz". */
		public final java.util.function.Function<String, String> displayName;

		public ListEdit(String name, String description, Supplier<Map<String, EspEntry>> map, Supplier<Collection<String>> options,
				java.util.function.Function<String, String> displayName) {
			super(name, description);
			this.map = map;
			this.options = options;
			this.displayName = displayName;
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
