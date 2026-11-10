package de.erleichterungen.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;

public final class Chat {
	private Chat() {
	}

	public static MutableComponent prefix(String module) {
		return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.literal(module).withStyle(ChatFormatting.LIGHT_PURPLE))
				.append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
	}

	public static void send(String module, Component message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		mc.player.sendSystemMessage(prefix(module).append(message));
	}

	public static void send(String module, String message) {
		send(module, Component.literal(message).withStyle(ChatFormatting.GRAY));
	}

	public static void sound(SoundEvent event, float pitch) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
	}
}
