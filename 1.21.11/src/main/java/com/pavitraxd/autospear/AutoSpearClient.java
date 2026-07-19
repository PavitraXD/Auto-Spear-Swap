package com.pavitraxd.autospear;

import com.pavitraxd.autospear.mixin.InventoryAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.Set;

public class AutoSpearClient implements ClientModInitializer {

	public static final String MOD_ID = "autospear";

	private static final Set<String> SPEAR_IDS = Set.of(
			"minecraft:wooden_spear",
			"minecraft:stone_spear",
			"minecraft:copper_spear",
			"minecraft:iron_spear",
			"minecraft:golden_spear",
			"minecraft:diamond_spear",
			"minecraft:netherite_spear"
	);

	public static KeyMapping triggerKey;

	@Override
	public void onInitializeClient() {
		triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.autospear.trigger",
				GLFW.GLFW_KEY_R,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autospear", "autospear"))
		));
		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	private void onTick(Minecraft client) {
		if (client.player == null) return;

		if (triggerKey.consumeClick() && client.screen == null) {
			useSpearInstant(client);
		}
	}

	private void useSpearInstant(Minecraft client) {
		int spearSlot = findLungeSpearSlot(client);
		if (spearSlot == -1) {
			client.player.displayClientMessage(
					Component.literal("§c[AutoSpear] No Lunge Spear found in hotbar."),
					true
			);
			return;
		}

		Inventory inventory = client.player.getInventory();
		InventoryAccessor accessor = (InventoryAccessor) inventory;
		int originalSlot = accessor.getSelected();

		// Swap to spear
		accessor.setSelected(spearSlot);
		// Sync slot to server immediately
		client.gameMode.ensureHasSentCarriedItem();
		// Fire the attack
		client.startAttack();
		// Swap back
		accessor.setSelected(originalSlot);
		client.gameMode.ensureHasSentCarriedItem();
	}

	private int findLungeSpearSlot(Minecraft client) {
		Inventory inventory = client.player.getInventory();
		for (int slot = 0; slot <= 8; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (isSpearWithLunge(stack)) return slot;
		}
		return -1;
	}

	private boolean isSpearWithLunge(ItemStack stack) {
		if (stack.isEmpty()) return false;

		Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (itemId == null || !SPEAR_IDS.contains(itemId.toString())) return false;

		ItemEnchantments enchantments = stack.getEnchantments();
		if (enchantments.isEmpty()) return false;

		for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
			var keyOpt = entry.getKey().unwrapKey();
			if (keyOpt.isPresent() && keyOpt.get().identifier().getPath().equals("lunge")) {
				return true;
			}
		}
		return false;
	}
}
