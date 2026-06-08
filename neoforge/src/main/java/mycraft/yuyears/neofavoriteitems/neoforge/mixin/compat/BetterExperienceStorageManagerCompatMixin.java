package mycraft.yuyears.neofavoriteitems.neoforge.mixin.compat;

import mycraft.yuyears.neofavoriteitems.DebugLogger;
import mycraft.yuyears.neofavoriteitems.application.BetterExperienceCompatService;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.github.edg_thexu.better_experience.module.faststorage.StorageManager", remap = false)
public abstract class BetterExperienceStorageManagerCompatMixin {
    private static final ThreadLocal<Inventory> neoFavoriteItems$activeInventory = new ThreadLocal<>();

    @Shadow
    private static void saveItemStack(ItemStack stack, ItemStack containerStack, int k) {
        throw new AssertionError();
    }

    @Inject(method = "saveAll", at = @At("HEAD"))
    private static void neoFavoriteItems$beginSaveAll(Player player, CallbackInfo ci) {
        if (player != null && !player.level().isClientSide()) {
            neoFavoriteItems$activeInventory.set(player.getInventory());
        }
    }

    @Inject(method = "saveAll", at = @At("RETURN"))
    private static void neoFavoriteItems$endSaveAll(Player player, CallbackInfo ci) {
        neoFavoriteItems$activeInventory.remove();
    }

    @Redirect(
        method = "saveAll",
        at = @At(
            value = "INVOKE",
            target = "Lcom/github/edg_thexu/better_experience/module/faststorage/StorageManager;saveItemStack(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)V"
        )
    )
    private static void neoFavoriteItems$skipLockedSourceStack(ItemStack sourceStack, ItemStack containerStack, int offsetZ) {
        Inventory inventory = neoFavoriteItems$activeInventory.get();
        if (BetterExperienceCompatService.shouldSkipPlayerInventorySource(inventory, sourceStack)) {
            DebugLogger.debug("BetterExperience fast storage skipped locked player inventory source stack");
            return;
        }
        saveItemStack(sourceStack, containerStack, offsetZ);
    }
}
