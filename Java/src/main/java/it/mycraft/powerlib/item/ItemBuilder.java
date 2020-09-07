package it.mycraft.powerlib.item;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import it.mycraft.powerlib.PowerLib;
import it.mycraft.powerlib.reflection.ReflectionAPI;
import it.mycraft.powerlib.utils.ColorAPI;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ItemBuilder {

    //TODO: Add addEnchant method

    private Material material;

    private String name;
    private List<String> lore;
    private int amount;
    private short metadata;
    private boolean glowing;

    public ItemBuilder() {
        amount = 1;
        metadata = 0;
    }

    /**
     * Sets the item's material
     *
     * @param material The material
     * @return The ItemBuilder
     */
    public ItemBuilder setMaterial(Material material) {
        this.material = material;
        return this;
    }

    /**
     * Sets the item's material getting it from a String
     *
     * @param material The material
     * @return The ItemBuilder
     */
    public ItemBuilder setMaterial(String material) {
        Optional<Material> optMaterial = Enums.getIfPresent(Material.class, material);
        if (optMaterial.isPresent())
            this.material = optMaterial.get();
        return this;
    }

    /**
     * Sets the item's material getting it from a String
     *
     * @param id The ID
     * @return The ItemBuilder
     */
    public ItemBuilder setMaterial(int id) {
        if (ReflectionAPI.getNumericalVersion() <= 12) {
            this.material = Material.getMaterial(id);
        } else {
            this.material = LegacyItemAPI.getMaterial(id);
        }
        return this;
    }

    /**
     * Sets the item's material getting it from a String
     *
     * @param id The ID
     * @return The ItemBuilder
     */
    public ItemBuilder setMaterial(int id, int data) {
        if (ReflectionAPI.getNumericalVersion() <= 12) {
            this.material = Material.getMaterial(id);
        } else {
            this.material = LegacyItemAPI.getMaterial(id, data);
        }
        return this;
    }

    /**
     * Sets the item's custom name
     *
     * @param name The name
     * @return The ItemBuilder
     */
    public ItemBuilder setName(String name) {
        this.name = ColorAPI.color(name);
        return this;
    }

    /**
     * Sets the item's lore Array
     *
     * @param lore The lore array
     * @return The ItemBuilder
     */
    public ItemBuilder setLore(String... lore) {
        setLore(Arrays.asList(lore));
        return this;
    }

    /**
     * Sets the item's lore List
     *
     * @param lore The lore list
     * @return The ItemBuilder
     */
    public ItemBuilder setLore(List<String> lore) {
        this.lore = ColorAPI.color(lore);
        return this;
    }

    /**
     * Sets the item's amount
     *
     * @param amount The amount
     * @return The ItemBuilder
     */
    public ItemBuilder setAmount(int amount) {
        this.amount = amount;
        return this;
    }

    /**
     * Dets the item's metadata (for older Minecraft versions)
     *
     * @param metadata The metadata
     * @return The ItemBuilder
     */
    public ItemBuilder setMetaData(short metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Sets the item's glowing
     *
     * @param glowing True to set item glowing
     * @return The ItemBuilder
     */
    public ItemBuilder setGlowing(boolean glowing) {
        this.glowing = glowing;
        return this;
    }

    /**
     * Clones another itemstack into the builder and stores its data
     *
     * @param itemStack The ItemStack to clone
     * @return The ItemBuilder
     */
    public ItemBuilder clone(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        material = itemStack.getType();
        amount = itemStack.getAmount();
        metadata = itemStack.getDurability();

        if (itemMeta.getDisplayName() != null)
            name = itemMeta.getDisplayName();

        if (itemMeta.hasLore())
            lore = itemMeta.getLore();

        if (itemMeta.hasEnchants() && itemMeta.getItemFlags().contains(ItemFlag.HIDE_ENCHANTS))
            glowing = true;

        return this;
    }

    /**
     * Sets a custom skin to a skull
     *
     * @param itemBuilder The itemBuilder
     * @param base64      The base64
     * @return The skull with a non-player skin
     */
    private ItemStack setCustomSkin(ItemBuilder itemBuilder, String base64) {
        String url = "http://textures.minecraft.net/texture/" + base64;
        int version = ReflectionAPI.getNumericalVersion();
        String material = version >= 13 ? "PLAYER_HEAD" : "SKULL_ITEM";
        ItemStack skull = itemBuilder.setMaterial(material).setMetaData((byte) 3).setAmount(1).setName(name).setLore(lore).build();
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        byte[] encodedData;

        if (version >= 14) {
            encodedData = org.bukkit.craftbukkit.libs.org.apache.commons.codec.binary.Base64
                    .encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
        } else {
            encodedData = org.apache.commons.codec.binary.Base64
                    .encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
        }
        profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
            PowerLib.getInstance().getLogger().log(Level.WARNING, "Error while creating an head with custom skin", ex);
            return null;
        }

        skull.setItemMeta(skullMeta);
        return skull;
    }

    /**
     * Sets a player's skin to a skull
     *
     * @param itemBuilder The itemBuilder
     * @param playerName  The player name
     * @return The skull with a player skin
     */
    private ItemStack setPlayerSkin(ItemBuilder itemBuilder, String playerName) {
        int version = ReflectionAPI.getNumericalVersion();
        String material = version >= 13 ? "PLAYER_HEAD" : "SKULL_ITEM";
        ItemStack skull = itemBuilder
                .setMaterial(material)
                .setAmount(1)
                .setMetaData((short) 3)
                .setName(name)
                .setLore(lore)
                .build();

        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwner(playerName);
        skull.setItemMeta(skullMeta);

        return skull;
    }

    /**
     * Sets a custom RGB color to a leather armor
     *
     * @param red         The red value
     * @param green       The green value
     * @param blue        The blue value
     * @param itemBuilder The itemBuilder
     * @return The armor with a custom color
     */
    private ItemStack setColorToArmor(int red, int green, int blue, ItemBuilder itemBuilder) {
        ItemStack leatherArmor = itemBuilder.setMaterial(material).setName(name).setLore(lore).build();
        LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) leatherArmor.getItemMeta();
        Color color = Color.fromRGB(red, green, blue);

        leatherArmorMeta.setColor(color);
        leatherArmor.setItemMeta(leatherArmorMeta);
        return leatherArmor;
    }

    /**
     * Adds a custom value on a placeholder to the item's name and lore
     *
     * @param placeholder The placeholder
     * @param value       The value
     * @return The ItemBuilder
     */
    public ItemBuilder addPlaceHolder(String placeholder, Object value) {
        name = name.replaceAll(placeholder, value.toString());

        List<String> newLore = new ArrayList<>();
        lore.forEach((s) -> newLore.add(s.replaceAll(placeholder, value.toString())));

        lore = newLore;
        return this;
    }

    /**
     * Builds an Itemstack with the data provided previously
     *
     * @return The ItemStack
     */
    public ItemStack build() {
        ItemStack itemStack = new ItemStack(material, amount, metadata);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (name != null)
            itemMeta.setDisplayName(name);

        if (lore != null)
            itemMeta.setLore(lore);

        if (glowing) {
            itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        itemStack.setItemMeta(itemMeta);
        reset();
        return itemStack;
    }

    /**
     * Uses the skin URL to make a custom skull
     *
     * @param skinURL The skin url
     * @return The ItemStack
     */
    public ItemStack customHeadBuild(String skinURL) {
        return setCustomSkin(this, skinURL);
    }

    /**
     * Uses the player's name to make a player skull
     *
     * @param playerName The player name
     * @return The ItemStack
     */
    public ItemStack playerHeadBuild(String playerName) {
        return setPlayerSkin(this, playerName);
    }

    /**
     * Uses RGB color values to set a custom color to a leather armor
     *
     * @param red   The red value
     * @param green The green value
     * @param blue  The blue value
     * @return The ItemStack
     */
    public ItemStack coloredArmorBuild(int red, int green, int blue) {
        return setColorToArmor(red, green, blue, this);
    }

    /**
     * Build a potion with the data privided
     *
     * @param type     The effect type
     * @param duration The duration measured in ticks
     * @param level    The amplifier
     * @return The potion
     */
    public ItemStack potionBuild(PotionEffectType type, int duration, int level) {
        return potionBuild(type, duration, level, true, true, true);
    }

    /**
     * Build a potion with the data privided
     *
     * @param type      The potion effect type to add
     * @param duration  The duration measured in ticks
     * @param level     The amplifier
     * @param overwrite true if any existing effect of the same type should be overwritten
     * @return The potion
     */
    public ItemStack potionBuild(PotionEffectType type, int duration, int level, boolean overwrite) {
        return potionBuild(type, duration, level, overwrite, true, true);
    }

    /**
     * Build a potion with the data privided
     *
     * @param type      The potion effect type to add
     * @param duration  The duration measured in ticks
     * @param level     The amplifier
     * @param overwrite true if any existing effect of the same type should be overwritten
     * @param ambient   The ambient status
     * @return The potion
     */
    public ItemStack potionBuild(PotionEffectType type, int duration, int level, boolean overwrite, boolean ambient) {
        return potionBuild(type, duration, level, overwrite, ambient, true);
    }

    /**
     * Build a potion with the data privided
     *
     * @param type      The potion effect type to add
     * @param duration  The duration measured in ticks
     * @param level     The amplifier
     * @param overwrite true if any existing effect of the same type should be overwritten
     * @param ambient   The ambient status
     * @param particles The particle status
     * @return The potion
     */
    public ItemStack potionBuild(PotionEffectType type, int duration, int level, boolean overwrite, boolean ambient, boolean particles) {
        ItemStack potion;
        potion = new ItemBuilder()
                .setMaterial(373)
                .setName(name)
                .setLore(lore)
                .setAmount(amount)
                .setMetaData(metadata)
                .setGlowing(glowing)
                .build();

        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, duration, (level - 1), ambient, particles), overwrite);
        potion.setItemMeta(meta);

        return potion;
    }

    /**
     * Looks for any item's info in the provided FileConfiguration and builds an ItemStack from it
     *
     * @param fileConfiguration The file configuration to get the item's info from
     * @param path              The section where the item's info are stored
     * @return The built ItemStack
     */
    public ItemStack fromConfig(FileConfiguration fileConfiguration, String path) {
        boolean legacy = false, glowing = false;
        String newPath, material = "STONE", name = null;
        List<String> lore = null;
        int amount = 1;
        short metadata = 0;

        for (String s : fileConfiguration.getConfigurationSection(path).getKeys(false)) {
            switch (s) {
                case "legacy":
                    newPath = path + ".legacy";
                    legacy = fileConfiguration.getBoolean(newPath);
                    break;
                case "material":
                    newPath = path + ".material";
                    material = legacy ? "LEGACY_" + fileConfiguration.getString(newPath) : fileConfiguration.getString(newPath);
                    break;
                case "name":
                    newPath = path + ".name";
                    name = fileConfiguration.getString(newPath);
                    break;
                case "lore":
                    newPath = path + ".lore";
                    lore = fileConfiguration.getStringList(newPath);
                    break;
                case "amount":
                    newPath = path + ".amount";
                    amount = fileConfiguration.getInt(newPath);
                    break;
                case "metadata":
                    newPath = path + ".metadata";
                    metadata = (short) fileConfiguration.getInt(newPath);
                    break;
                case "glowing":
                    newPath = path + ".glowing";
                    glowing = fileConfiguration.getBoolean(newPath);
                    break;
                default:
                    break;
            }
        }
        return new ItemBuilder()
                .setMaterial(material)
                .setName(name)
                .setLore(lore)
                .setAmount(amount)
                .setMetaData(metadata)
                .setGlowing(glowing)
                .build();
    }

    /**
     * Just puts in the ItemBuilder object its default values
     */
    private void reset() {
        material = null;
        name = null;
        lore = null;

        amount = 1;
        metadata = 0;

        glowing = false;
    }
}
