package de.erleichterungen.gui;

import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Beschreibung eines Moduls für die GUI.
 */
public final class Module {
	public enum Category {
		RENDER("Render"),
		WORLD("Welt"),
		PLAYER("Spieler"),
		MISC("Sonstiges"),
		CLIENT("Client");

		public final String title;

		Category(String title) {
			this.title = title;
		}
	}

	public final String id;
	public final String name;
	public final Category category;
	public final String description;
	/** null = Modul hat keinen An/Aus-Zustand (nur Einstellungen). */
	public final BooleanSupplier enabled;
	public final Runnable toggle;
	public final Supplier<KeyMapping> key;
	public final List<Setting> settings = new ArrayList<>();

	public Module(String id, String name, Category category, String description, BooleanSupplier enabled, Runnable toggle,
			Supplier<KeyMapping> key) {
		this.id = id;
		this.name = name;
		this.category = category;
		this.description = description;
		this.enabled = enabled;
		this.toggle = toggle;
		this.key = key;
	}

	public Module add(Setting setting) {
		settings.add(setting);
		return this;
	}

	public boolean isEnabled() {
		return enabled != null && enabled.getAsBoolean();
	}

	public boolean hasToggle() {
		return enabled != null;
	}
}
