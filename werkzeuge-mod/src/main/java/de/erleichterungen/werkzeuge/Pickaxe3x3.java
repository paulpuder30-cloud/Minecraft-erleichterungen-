package de.erleichterungen.werkzeuge;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Spitzhacke, die eine senkrechte 3x3-Fläche quer zur Blickrichtung abbaut
 * (Blick nach Norden/Süden → Fläche in X/Y, Blick nach Osten/Westen → Fläche in Z/Y).
 * Schleichen baut nur den einen Block ab.
 */
public class Pickaxe3x3 extends Item {
	/** Nachbarblöcke dürfen höchstens so viel härter sein als der angeschlagene Block (kein Obsidian aus Versehen). */
	private static final float MAX_EXTRA_HARDNESS = 2.0f;
	private static boolean breaking;

	public Pickaxe3x3(Properties properties) {
		super(properties);
	}

	public static void afterBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (breaking || level.isClientSide() || !(player instanceof ServerPlayer sp)) return;
		ItemStack tool = player.getMainHandItem();
		if (!tool.is(WerkzeugeMod.PICKAXE_3X3) || player.isShiftKeyDown()) return;
		float centerHardness = state.getDestroySpeed(level, pos);
		Direction facing = player.getDirection();
		breaking = true;
		try {
			for (int a = -1; a <= 1; a++) {
				for (int b = -1; b <= 1; b++) {
					if (a == 0 && b == 0) continue;
					BlockPos p = facing.getAxis() == Direction.Axis.X ? pos.offset(0, b, a) : pos.offset(a, b, 0);
					BlockState s = level.getBlockState(p);
					float hardness = s.getDestroySpeed(level, p);
					if (s.isAir() || hardness < 0 || hardness > centerHardness + MAX_EXTRA_HARDNESS) continue;
					if (!tool.isCorrectToolForDrops(s)) continue;
					if (tool.isEmpty() || !player.getMainHandItem().is(WerkzeugeMod.PICKAXE_3X3)) return; // Werkzeug kaputt
					// voller Abbau wie von Hand: Drops mit Verzauberungen, Haltbarkeit, Events
					sp.gameMode.destroyBlock(p);
				}
			}
		} finally {
			breaking = false;
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		out.accept(Component.translatable("item.werkzeuge.pickaxe_3x3.desc1").withStyle(ChatFormatting.GRAY));
		out.accept(Component.translatable("item.werkzeuge.pickaxe_3x3.desc2").withStyle(ChatFormatting.DARK_GRAY));
	}
}
