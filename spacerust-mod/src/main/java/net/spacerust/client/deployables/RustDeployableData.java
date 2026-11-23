package net.spacerust.client.deployables;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;

import java.util.*;

public class RustDeployableData {
    public enum LockType { NONE, KEY_LOCK, CODE_LOCK }

    public String id;
    public RustDeployableType type;
    public double x, y, z;
    public float yaw;
    public float health;
    public float maxHealth;
    public long placedTime;

    // Door state
    public boolean isOpen = false;
    public transient float openProgress = 0f; // 0.0 to 1.0 animation

    // Lock state
    public LockType lockType = LockType.NONE;
    public boolean isLocked = false;
    public String lockCode = "";
    public String ownerUuid = "";
    public Set<String> authorizedPlayers = new HashSet<>();

    // Container Inventory (TC, Small Box, Large Box)
    public List<Map<String, Object>> serializedInventory = new ArrayList<>();
    public transient List<ItemStack> inventory = new ArrayList<>();

    // Tool Cupboard Upkeep & Decay
    public long protectedUntil = 0;
    public long lastUpkeepCheck = 0;

    public RustDeployableData() {}

    public RustDeployableData(RustDeployableType type, double x, double y, double z, float yaw, String ownerUuid) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.health = type.maxHealth;
        this.maxHealth = type.maxHealth;
        this.placedTime = System.currentTimeMillis();
        this.ownerUuid = ownerUuid != null ? ownerUuid : "";
        if (ownerUuid != null && !ownerUuid.isEmpty()) {
            this.authorizedPlayers.add(ownerUuid);
        }

        int slotCount = (type == RustDeployableType.SMALL_BOX) ? 12 :
                ((type == RustDeployableType.LARGE_BOX) ? 30 :
                        ((type == RustDeployableType.TOOL_CUPBOARD) ? 24 : 0));
        for (int i = 0; i < slotCount; i++) {
            inventory.add(ItemStack.EMPTY);
        }
    }

    public boolean isAuthorized(String playerUuid) {
        if (playerUuid == null || playerUuid.isEmpty()) return true;
        if (!isLocked) return true;
        if (lockType == LockType.KEY_LOCK) {
            return ownerUuid.equals(playerUuid);
        } else if (lockType == LockType.CODE_LOCK) {
            return authorizedPlayers.contains(playerUuid) || ownerUuid.equals(playerUuid);
        }
        return true;
    }

    public Box getBoundingBox() {
        if (type.isDoor) {
            if (isOpen) {
                // When open, narrow bounding box along wall
                return new Box(x - 0.2, y, z - 0.2, x + 0.2, y + 2.2, z + 0.2);
            } else {
                // When closed, blocks the doorway
                float normYaw = (yaw % 360f + 360f) % 360f;
                if ((normYaw > 45 && normYaw < 135) || (normYaw > 225 && normYaw < 315)) {
                    return new Box(x - 0.15, y, z - 0.65, x + 0.15, y + 2.2, z + 0.65);
                } else {
                    return new Box(x - 0.65, y, z - 0.15, x + 0.65, y + 2.2, z + 0.15);
                }
            }
        } else if (type == RustDeployableType.TOOL_CUPBOARD) {
            return new Box(x - 0.55, y, z - 0.45, x + 0.55, y + 1.9, z + 0.45);
        } else if (type == RustDeployableType.SMALL_BOX) {
            return new Box(x - 0.45, y, z - 0.35, x + 0.45, y + 0.55, z + 0.35);
        } else if (type == RustDeployableType.LARGE_BOX) {
            return new Box(x - 0.9, y, z - 0.5, x + 0.9, y + 0.8, z + 0.5);
        } else if (type == RustDeployableType.CAMPFIRE) {
            return new Box(x - 0.45, y, z - 0.45, x + 0.45, y + 0.35, z + 0.45);
        } else if (type == RustDeployableType.FURNACE) {
            return new Box(x - 0.55, y, z - 0.55, x + 0.55, y + 1.45, z + 0.55);
        } else if (type == RustDeployableType.SLEEPING_BAG) {
            return new Box(x - 0.5, y, z - 0.9, x + 0.5, y + 0.2, z + 0.9);
        } else if (type == RustDeployableType.WORKBENCH_1 || type == RustDeployableType.WORKBENCH_2 || type == RustDeployableType.WORKBENCH_3) {
            return new Box(x - 0.8, y, z - 0.45, x + 0.8, y + 1.1, z + 0.45);
        }
        return new Box(x - 0.3, y, z - 0.3, x + 0.3, y + 0.5, z + 0.3);
    }
}
