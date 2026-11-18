package de.erleichterungen.werkzeuge;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * Zwei Werkzeuge:
 * <ul>
 *   <li><b>3x3-Spitzhacke</b> – baut eine senkrechte 3x3-Fläche quer zur Blickrichtung ab (Schleichen = nur ein Block).</li>
 *   <li><b>Hyperion</b> – Schwert mit der Fähigkeit "Wither Impact" (Rechtsklick): 10 Blöcke Teleport,
 *   Implosion um den Landepunkt und Wither-Schild.</li>
 * </ul>
 * Die Mod muss auf dem Server (bzw. im Einzelspieler) installiert sein, da sie neue Items hinzufügt.
 */
public class WerkzeugeMod implements ModInitializer {
	public static final String MOD_ID = "werkzeuge";
	public static final Logger LOGGER = LoggerFactory.getLogger("Werkzeuge");

	public static Item PICKAXE_3X3;
	public static Item HYPERION;

	@Override
	public void onInitialize() {
		PICKAXE_3X3 = register("pickaxe_3x3", Pickaxe3x3::new, new Item.Properties()
				.pickaxe(ToolMaterial.NETHERITE, 1.0f, -2.8f)
				.fireResistant()
				.rarity(Rarity.EPIC));
		HYPERION = register("hyperion", HyperionItem::new, new Item.Properties()
				.sword(ToolMaterial.NETHERITE, 8.0f, -2.4f)
				.fireResistant()
				.rarity(Rarity.EPIC));

		CreativeModeTabEvents.modifyOutputEvent(tab("tools_and_utilities")).register(out -> out.accept(PICKAXE_3X3));
		CreativeModeTabEvents.modifyOutputEvent(tab("combat")).register(out -> out.accept(HYPERION));

		PlayerBlockBreakEvents.AFTER.register(Pickaxe3x3::afterBreak);
		ServerTickEvents.END_SERVER_TICK.register(HyperionItem::serverTick);

		LOGGER.info("Werkzeuge geladen: 3x3-Spitzhacke, Hyperion");
	}

	private static ResourceKey<net.minecraft.world.item.CreativeModeTab> tab(String name) {
		return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(name));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
	}
}
