package net.spacerust.client.gui;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Helper for 1000-size Rust resource stacks synced via NBT
 * (supports both SpaceRustCore Bukkit PersistentDataContainer and local SP tags).
 */
public final class RustStackUtil {

    private RustStackUtil() {}

    public static int getCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        NbtCompound tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("SpaceRustCount")) {
                return Math.max(1, tag.getInt("SpaceRustCount"));
            }
            if (tag.contains("PublicBukkitValues")) {
                NbtCompound pub = tag.getCompound("PublicBukkitValues");
                if (pub.contains("spacerustcore:rcount")) {
                    return Math.max(1, pub.getInt("spacerustcore:rcount"));
                }
            }
        }
        return stack.getCount();
    }

    public static boolean isCustomStacked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NbtCompound tag = stack.getTag();
        if (tag == null) return false;
        if (tag.contains("SpaceRustCount")) return true;
        return tag.contains("PublicBukkitValues")
                && tag.getCompound("PublicBukkitValues").contains("spacerustcore:rcount");
    }

    public static void setLocalCount(ItemStack stack, int count) {
        if (stack == null || stack.isEmpty()) return;
        if (count > 1) {
            stack.getOrCreateTag().putInt("SpaceRustCount", Math.min(1000, count));
            stack.setCount(1);
        } else {
            if (stack.hasTag()) stack.getTag().remove("SpaceRustCount");
            stack.setCount(1);
        }
    }
}
