package net.spacerust.client.guns;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3f;
import net.spacerust.client.building.RbmModel;

public class RustHeldItemManager {

    public static String getHeldModel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String name = stack.getName().getString().toLowerCase();
        Item item = stack.getItem();

        // 1. Building Plan
        if (item == Items.PAPER || name.contains("план") || name.contains("plan") || name.contains("blueprint")) {
            return "held_plan";
        }

        // 2. Hammer
        if (item == Items.WOODEN_AXE || item == Items.WOODEN_SHOVEL || name.contains("молоток") || name.contains("hammer")) {
            return "held_hammer";
        }

        // 3. Firearms
        if (name.contains("ak47") || name.contains("ак-47") || name.contains("калаш") || (item == Items.NETHERITE_SWORD && name.contains("ak"))) {
            return "held_ak";
        }
        if (name.contains("thompson") || name.contains("томпсон") || (item == Items.IRON_SWORD && name.contains("томпсон"))) {
            return "held_thompson";
        }
        if (name.contains("sar") || (item == Items.STONE_HOE && name.contains("sar"))) {
            return "held_sar";
        }
        if (name.contains("mp5") || (item == Items.GOLDEN_SWORD && name.contains("mp5"))) {
            return "held_mp5";
        }
        if (name.contains("bolt") || name.contains("болт") || (item == Items.DIAMOND_HOE && name.contains("bolt"))) {
            return "held_bolt";
        }
        if (name.contains("l96") || (item == Items.NETHERITE_HOE && name.contains("l96"))) {
            return "held_l96";
        }
        if (name.contains("m249")) {
            return "held_m249";
        }
        if (name.contains("revolver") || name.contains("револьвер") || item == Items.BOW) {
            return "held_revolver";
        }
        if (name.contains("python") || name.contains("питон")) {
            return "held_python";
        }
        if (name.contains("semipistol") || name.contains("sap")) {
            return "held_semipistol";
        }
        if (name.contains("pump") || name.contains("помпов") || (item == Items.STONE_SHOVEL && name.contains("дробовик"))) {
            return "held_pump_shotgun";
        }
        if (name.contains("двуствол") || name.contains("doublebarrel") || name.contains("pipe_shotgun")) {
            return "held_doublebarrel";
        }
        if (name.contains("waterpipe") || name.contains("пайп")) {
            return "held_waterpipe";
        }
        if (name.contains("eoka") || name.contains("эока")) {
            return "held_eoka";
        }
        if (name.contains("c4")) {
            return "held_c4";
        }

        // 4. Archery
        if (item == Items.CROSSBOW || name.contains("арбалет")) {
            return "held_crossbow";
        }
        if (name.contains("лук") || name.contains("hunting_bow")) {
            return "held_hunting_bow";
        }

        // 5. Tools & Medical
        if (name.contains("каменный топор") || (item == Items.STONE_AXE && name.contains("топор"))) {
            return "held_stone_hatchet";
        }
        if (name.contains("каменная кирка") || (item == Items.STONE_PICKAXE && name.contains("кирка"))) {
            return "held_stone_pickaxe";
        }
        if (name.contains("металлический топор") || (item == Items.IRON_AXE && name.contains("топор"))) {
            return "held_hatchet";
        }
        if (name.contains("металлическая кирка") || (item == Items.IRON_PICKAXE && name.contains("кирка"))) {
            return "held_pickaxe";
        }
        if (name.contains("шприц") || name.contains("syringe") || item == Items.POTION) {
            return "held_syringe";
        }
        if (name.contains("бинт") || name.contains("bandage")) {
            return "held_bandage";
        }
        if (item == Items.TORCH || name.contains("факел") || name.contains("torch")) {
            return "held_torch";
        }
        if (name.contains("копье") || name.contains("spear")) {
            return "held_wooden_spear";
        }
        if (item == Items.COBBLESTONE && (name.contains("камень") || name.contains("rock"))) {
            return "held_rock";
        }

        return null;
    }

    public static boolean renderFirstPerson(AbstractClientPlayerEntity player, ItemStack item, Hand hand,
                                            float swingProgress, float equipProgress,
                                            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (hand != Hand.MAIN_HAND) return false;
        String modelName = getHeldModel(item);
        if (modelName == null) return false;

        RbmModel rbm = RbmModel.get(modelName);
        if (rbm == null) return false;

        matrices.push();

        // Equip animation (lowers on equip)
        float equipOffset = -0.5f * (1.0f - (float) Math.sqrt(Math.max(0f, 1.0f - equipProgress)));
        matrices.translate(0, equipOffset, 0);

        boolean isGun = modelName.startsWith("held_ak") || modelName.startsWith("held_thompson") ||
                        modelName.startsWith("held_sar") || modelName.startsWith("held_mp5") ||
                        modelName.startsWith("held_bolt") || modelName.startsWith("held_revolver") ||
                        modelName.startsWith("held_pump") || modelName.startsWith("held_doublebarrel") ||
                        modelName.startsWith("held_l96") || modelName.startsWith("held_m249") ||
                        modelName.startsWith("held_eoka") || modelName.startsWith("held_waterpipe") ||
                        modelName.startsWith("held_python") || modelName.startsWith("held_semipistol");

        if (isGun) {
            float ads = RustShootingManager.adsProgress; // 0.0 (hip) to 1.0 (ADS)

            // Hipfire position
            float hipX = 0.16f;
            float hipY = -0.18f;
            float hipZ = -0.32f;

            // ADS center sight position
            float adsX = 0.00f;
            float adsY = -0.138f;
            float adsZ = -0.27f;

            float posX = hipX * (1.0f - ads) + adsX * ads;
            float posY = hipY * (1.0f - ads) + adsY * ads;
            float posZ = hipZ * (1.0f - ads) + adsZ * ads;

            matrices.translate(posX, posY, posZ);

            // Recoil kick back into camera
            float kick = RustRecoilManager.getVisualKick();
            matrices.translate(0, kick * 0.012f, kick * 0.035f);
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-kick * 1.5f));

        } else if (modelName.equals("held_plan")) {
            // Blueprint held in hands tilted forward
            matrices.translate(0.02f, -0.25f, -0.42f);
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(25.0f));
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-10.0f));

        } else if (modelName.equals("held_hammer")) {
            // Building hammer in right hand
            matrices.translate(0.18f, -0.18f, -0.32f);
            if (swingProgress > 0) {
                matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-swingProgress * 45.0f));
            }

        } else {
            // Melee tools (hatchet, pickaxe, rock, torch, syringe)
            matrices.translate(0.16f, -0.18f, -0.32f);
            if (swingProgress > 0) {
                matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-swingProgress * 40.0f));
            }
        }

        // Render authentic 3D Rust model
        rbm.render(matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);

        matrices.pop();
        return true;
    }
}
