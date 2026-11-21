package net.spacerust.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class RustCraftingQueue {

    public static class CraftTask {
        public final String name;
        public final ItemStack resultItem;
        public final int amount;
        public final float totalSeconds;
        public float remainingSeconds;

        public CraftTask(String name, ItemStack resultItem, int amount, float totalSeconds) {
            this.name = name;
            this.resultItem = resultItem;
            this.amount = amount;
            this.totalSeconds = totalSeconds;
            this.remainingSeconds = totalSeconds;
        }
    }

    public static final List<CraftTask> queue = new ArrayList<>();

    public static void addTask(String name, ItemStack item, int amount, float seconds) {
        if (queue.size() < 8) { // max 8 queue slots like in Rust
            queue.add(new CraftTask(name, item, amount, seconds));
        }
    }

    public static void tick(MinecraftClient client) {
        if (queue.isEmpty()) return;

        CraftTask current = queue.get(0);
        current.remainingSeconds -= 0.05f; // 20 ticks per second = 0.05s per tick

        if (current.remainingSeconds <= 0) {
            queue.remove(0);
            if (client.player != null) {
                // Play craft completion chime
                client.player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                // Give item if in survival or send craft command
                client.player.inventory.insertStack(current.resultItem.copy());
            }
        }
    }
}
