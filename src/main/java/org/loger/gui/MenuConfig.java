package org.loger.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MenuConfig {
    private final JavaPlugin plugin;
    private String menuTitle = "Курятник";
    private int size = 54;
    private List<Integer> playerSlots = new ArrayList();
    private String playerHeadDisplayName = "&x&0&0&C&1&F&F▶ &f{PLAYER} [{PROGRESS}] {PERCENT}";
    private List<String> playerHeadLore = new ArrayList();
    private Map<String, MenuItem> decorationItems = new HashMap();
    private int prevPageSlot = 45;
    private int nextPageSlot = 53;
    private String prevPageName = "&x&0&0&C&1&F&F▶ &fПредыдущая страница";
    private String nextPageName = "&x&0&0&C&1&F&F▶ &fСледующая страница";
    private Material prevPageMaterial = Material.ARROW;
    private Material nextPageMaterial = Material.ARROW;
    private int progressBarLength = 8;
    private String progressBarFilled = "■";
    private String progressBarEmpty = " ";
    private String progressBarFilledColor = "&#FF5300";
    private String progressBarEmptyColor = "&7";

    public static class MenuItem {
        public String displayName;
        public Material material;
        public List<Integer> slots = new ArrayList();
    }

    public MenuConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(this.plugin.getDataFolder(), "menu.yml");
        if (!file.exists()) {
            this.plugin.saveResource("menu.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.menuTitle = config.getString("menu_title", this.menuTitle);
        this.size = config.getInt("size", this.size);
        this.playerSlots = config.getIntegerList("player_slots");
        if (this.playerSlots.isEmpty()) {
            this.playerSlots = Arrays.asList(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43);
        }
        this.playerHeadDisplayName = config.getString("player_head.display_name", this.playerHeadDisplayName);
        this.playerHeadLore = config.getStringList("player_head.lore");
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    MenuItem item = new MenuItem();
                    item.material = Material.matchMaterial(itemSection.getString("material", "STONE"));
                    item.displayName = itemSection.getString("display_name", "&f");
                    item.slots = itemSection.getIntegerList("slots");
                    this.decorationItems.put(key, item);
                }
            }
        }
        ConfigurationSection navSection = config.getConfigurationSection("navigation");
        if (navSection != null) {
            ConfigurationSection prevSection = navSection.getConfigurationSection("previous_page");
            if (prevSection != null) {
                this.prevPageSlot = prevSection.getInt("slot", this.prevPageSlot);
                this.prevPageMaterial = Material.matchMaterial(prevSection.getString("material", "ARROW"));
                this.prevPageName = prevSection.getString("display_name", this.prevPageName);
            }
            ConfigurationSection nextSection = navSection.getConfigurationSection("next_page");
            if (nextSection != null) {
                this.nextPageSlot = nextSection.getInt("slot", this.nextPageSlot);
                this.nextPageMaterial = Material.matchMaterial(nextSection.getString("material", "ARROW"));
                this.nextPageName = nextSection.getString("display_name", this.nextPageName);
            }
        }
        ConfigurationSection progressSection = config.getConfigurationSection("progress_bar");
        if (progressSection != null) {
            this.progressBarLength = progressSection.getInt("length", this.progressBarLength);
            this.progressBarFilled = progressSection.getString("filled", this.progressBarFilled);
            this.progressBarEmpty = progressSection.getString("empty", this.progressBarEmpty);
            this.progressBarFilledColor = progressSection.getString("filled_color", this.progressBarFilledColor);
            this.progressBarEmptyColor = progressSection.getString("empty_color", this.progressBarEmptyColor);
        }
    }

    public String getMenuTitle() {
        return this.menuTitle;
    }

    public int getSize() {
        return this.size;
    }

    public List<Integer> getPlayerSlots() {
        return this.playerSlots;
    }

    public String getPlayerHeadDisplayName() {
        return this.playerHeadDisplayName;
    }

    public List<String> getPlayerHeadLore() {
        return this.playerHeadLore;
    }

    public Map<String, MenuItem> getDecorationItems() {
        return this.decorationItems;
    }

    public int getPrevPageSlot() {
        return this.prevPageSlot;
    }

    public int getNextPageSlot() {
        return this.nextPageSlot;
    }

    public String getPrevPageName() {
        return this.prevPageName;
    }

    public String getNextPageName() {
        return this.nextPageName;
    }

    public Material getPrevPageMaterial() {
        return this.prevPageMaterial;
    }

    public Material getNextPageMaterial() {
        return this.nextPageMaterial;
    }

    public int getProgressBarLength() {
        return this.progressBarLength;
    }

    public String getProgressBarFilled() {
        return this.progressBarFilled;
    }

    public String getProgressBarEmpty() {
        return this.progressBarEmpty;
    }

    public String getProgressBarFilledColor() {
        return this.progressBarFilledColor;
    }

    public String getProgressBarEmptyColor() {
        return this.progressBarEmptyColor;
    }
}
