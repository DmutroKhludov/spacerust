package net.spacerust.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.spacerust.client.world.RustWorldSync;

import java.util.ArrayList;
import java.util.List;

public class RustCraftingQueue {

    public static class CraftTask {
        public final String recipeId;
        public final String name;
        public final ItemStack resultItem;
        public int amount;
        public final float totalSeconds; // per-unit craft time for UI compatibility
        public float unitRemaining;
        public float remainingSeconds;

        public CraftTask(String recipeId, String name, ItemStack resultItem, int amount, float totalSeconds) {
            this.recipeId = recipeId;
            this.name = name;
            this.resultItem = resultItem;
            this.amount = Math.max(1, amount);
            this.totalSeconds = Math.max(0.5f, totalSeconds);
            this.unitRemaining = this.totalSeconds;
            this.remainingSeconds = this.totalSeconds * this.amount;
        }
    }

    public static final List<CraftTask> queue = new ArrayList<>();

    public static void clear() {
        queue.clear();
    }

    public static void addTask(String recipeId, String name, ItemStack item, int amount, float seconds) {
        if (queue.size() < 8) {
            queue.add(new CraftTask(recipeId, name, item, amount, seconds));
        }
    }

    public static int getTotalRemainingCount() {
        int sum = 0;
        for (CraftTask t : queue) {
            sum += Math.max(0, t.amount);
        }
        return sum;
    }

    public static float getTotalRemainingSeconds() {
        float sum = 0f;
        for (CraftTask t : queue) {
            sum += Math.max(0f, t.remainingSeconds);
        }
        return sum;
    }

    public static float getActiveUnitProgress() {
        if (queue.isEmpty()) return 0f;
        CraftTask t = queue.get(0);
        return Math.max(0f, Math.min(1f, 1f - (t.unitRemaining / Math.max(0.1f, t.totalSeconds))));
    }

    public static void tick(MinecraftClient client) {
        if (queue.isEmpty()) return;

        CraftTask current = queue.get(0);
        current.unitRemaining -= 0.05f;
        current.remainingSeconds = Math.max(0f, current.unitRemaining + Math.max(0, current.amount - 1) * current.totalSeconds);

        if (current.unitRemaining <= 0.001f) {
            if (client != null && client.player != null) {
                if (client.isInSingleplayer()) {
                    ItemStack one = current.resultItem.copy();
                    one.setCount(1);
                    client.player.inventory.insertStack(one);
                } else {
                    RustWorldSync.sendCraftComplete(current.recipeId, 1);
                }
            }
            current.amount--;
            if (current.amount <= 0) {
                queue.remove(0);
            } else {
                current.unitRemaining += current.totalSeconds;
                current.remainingSeconds = Math.max(0f, current.unitRemaining + (current.amount - 1) * current.totalSeconds);
            }
        }
    }
}
