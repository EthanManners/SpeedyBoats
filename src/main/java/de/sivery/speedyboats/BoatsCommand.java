package de.sivery.speedyboats;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BoatsCommand implements CommandExecutor, Listener {
    private final SpeedyBoats plugin;
    private final Set<UUID> recipeViewers = new HashSet<>();

    public BoatsCommand(SpeedyBoats plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        openEnginesMenu(player);
        return true;
    }

    private void openEnginesMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new EnginesMenuHolder(), 27, Component.text("Boat Engines"));
        int slot = 0;
        for (Engine engine : Engine.REGISTERED) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, engine.getItem());
        }

        player.openInventory(inventory);
    }

    private void openRecipeMenu(Player player, Engine engine) {
        if (!(engine.getRecipe() instanceof ShapedRecipe shapedRecipe)) {
            player.sendMessage("No recipe available for this engine.");
            return;
        }

        player.openWorkbench(null, true);
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (!(inventory instanceof CraftingInventory craftingInventory)) {
            return;
        }

        String[] shape = shapedRecipe.getShape();
        Map<Character, RecipeChoice> choiceMap = shapedRecipe.getChoiceMap();
        Map<Character, ItemStack> ingredientMap = new HashMap<>();

        for (Map.Entry<Character, RecipeChoice> entry : choiceMap.entrySet()) {
            ItemStack ingredient = ingredientFromChoice(entry.getValue());
            if (ingredient != null) {
                ingredientMap.put(entry.getKey(), ingredient);
            }
        }

        ItemStack[] matrix = new ItemStack[9];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                char c = shape[row].charAt(col);
                if (c != ' ') {
                    matrix[(row * 3) + col] = ingredientMap.get(c);
                }
            }
        }

        craftingInventory.setMatrix(matrix);
        craftingInventory.setResult(engine.getItem());
        recipeViewers.add(player.getUniqueId());
    }

    private ItemStack ingredientFromChoice(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.ExactChoice exactChoice && !exactChoice.getChoices().isEmpty()) {
            return exactChoice.getChoices().get(0).clone();
        }

        if (choice instanceof RecipeChoice.MaterialChoice materialChoice && !materialChoice.getChoices().isEmpty()) {
            return new ItemStack(materialChoice.getChoices().get(0));
        }

        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();

        if (holder instanceof EnginesMenuHolder) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) {
                return;
            }

            String key = Engine.getEngineKey(plugin, clicked);
            if (key == null) {
                return;
            }

            Optional<Engine> engine = Engine.byKey(key);
            engine.ifPresent(value -> openRecipeMenu(player, value));
            return;
        }

        if (recipeViewers.contains(player.getUniqueId()) && topInventory.getType() == InventoryType.WORKBENCH) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        recipeViewers.remove(event.getPlayer().getUniqueId());
    }

    private static final class EnginesMenuHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, 27);
        }
    }
}
