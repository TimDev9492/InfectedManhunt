package me.timwastaken.infectedmanhunt.common;

import me.timwastaken.infectedmanhunt.InfectedManhunt;
import me.timwastaken.infectedmanhunt.exceptions.ItemBuildException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ItemUtils {
    private ItemUtils() {}

    public static boolean containsTag(ItemStack stack, String tag) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        String tagValue = meta.getPersistentDataContainer().get(
                new NamespacedKey(InfectedManhunt.getInstance(), tag),
                PersistentDataType.STRING
        );
        return Objects.equals(tagValue, "true");
    }

    public static void trackLocation(ItemStack stack, Location target) {
        switch (stack.getType()) {
            case Material.COMPASS -> {
                CompassMeta meta = (CompassMeta) stack.getItemMeta();
                if (meta == null) return;
                meta.setLodestoneTracked(false);
                meta.setLodestone(target);
                stack.setItemMeta(meta);
            }
            default -> {
                return;
            }
        }
    }

    public static void renameItem(ItemStack item, String displayName) {
        if (displayName == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
    }

    public static class Builder {
        private final Material material;
        private int amount = 1;
        private String displayName = null;
        private final List<String> tags;
        private final List<String> lore;
        private final List<Consumer<ItemMeta>> metaTransforms;
        private boolean unbreakable = false;

        public Builder(Material material) {
            this.material = material;
            this.tags = new ArrayList<>();
            this.lore = new ArrayList<>();
            this.metaTransforms = new ArrayList<>();
        }

        public Builder withDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder withAmount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder withTag(String... tagsToAdd) {
            tags.addAll(Arrays.asList(tagsToAdd));
            return this;
        }

        public Builder setUnbreakable(boolean unbreakable) {
            this.unbreakable = unbreakable;
            return this;
        }

        public Builder addMetaTransform(Consumer<ItemMeta> transform) {
            this.metaTransforms.add(transform);
            return this;
        }

        public Builder appendLore(String... loreLines) {
            lore.addAll(Arrays.asList(loreLines));
            return this;
        }

        public ItemStack build() throws ItemBuildException {
            ItemStack stack = new ItemStack(this.material, this.amount);
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) throw new ItemBuildException("ItemMeta is null!");
            PersistentDataContainer container = meta.getPersistentDataContainer();
            for (String tag : tags) {
                NamespacedKey key = new NamespacedKey(InfectedManhunt.getInstance(), tag);
                container.set(key, PersistentDataType.STRING, "true");
            }
            if (this.displayName != null) meta.setDisplayName(this.displayName);
            meta.setLore(this.lore);
            meta.setUnbreakable(unbreakable);
            for (Consumer<ItemMeta> transform : this.metaTransforms) {
                transform.accept(meta);
            }
            stack.setItemMeta(meta);
            return stack;
        }
    }
}
