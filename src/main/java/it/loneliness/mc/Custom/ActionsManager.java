package it.loneliness.mc.Custom;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import io.papermc.paper.event.player.PlayerTradeEvent;
import it.loneliness.mc.Plugin;
import it.loneliness.mc.Controller.Announcement;
import it.loneliness.mc.Controller.ScoreboardController;
import it.loneliness.mc.Model.LogHandler;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;

public class ActionsManager implements Listener {
    private static ActionsManager instance;

    Plugin plugin;
    LogHandler logger;
    Announcement announcement;
    List<Action> ongoingActions;
    LocalDateTime lastAddedActionTime;
    int newActionsMinutes;
    FileConfiguration config;
    ScoreboardController scoreboardController;

    List<String> rawCraftingActions;
    List<String> rawTradeActions;
    List<String> rawKillingActions;

    public static ActionsManager getInstance(Plugin plugin) {
        if (instance == null) {
            instance = new ActionsManager(plugin);
        }
        return instance;
    }

    private ActionsManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = LogHandler.getInstance(null);
        this.announcement = Announcement.getInstance(this.plugin);
        this.scoreboardController = ScoreboardController.getInstance(plugin);

        this.config = plugin.getConfig();

        this.newActionsMinutes = config.getInt("minutes-between-actions");

        this.rawCraftingActions = config.getStringList("craft-actions");
        this.rawTradeActions = config.getStringList("trade-actions");
        this.rawKillingActions = config.getStringList("kill-actions");

        this.ongoingActions = new ArrayList<>();
        if (this.plugin.getConfig().getBoolean("debug")) {

            for (String action : this.rawCraftingActions) {
                ongoingActions.add(getActionFromString(action, ActionType.CRAFT));
            }
            for (String action : this.rawTradeActions) {
                ongoingActions.add(getActionFromString(action, ActionType.TRADE));
            }
            for (String action : this.rawKillingActions) {
                ongoingActions.add(getActionFromString(action, ActionType.KILL));
            }
            //ongoingActions.add(new CraftAction(10, 100, 60, config, Material.TORCH));
            //ongoingActions.add(new TradeAction(10, 100, 60, config, Villager.Profession.FARMER));
            //ongoingActions.add(new KillAction(10, 100, 60, config, EntityType.ZOMBIE));
        }

    }

    // Try to call this function at least once per minute
    public void periodicRunner() {
        if (ongoingActions != null) {
            ongoingActions = ongoingActions.stream().filter(action -> action.getMinutesUntilEnd() > 0)
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            ongoingActions = new ArrayList<>();
        }

        // If i need to select a new action
        if (lastAddedActionTime == null || Duration.between(lastAddedActionTime, LocalDateTime.now())
                .toMinutes() > newActionsMinutes) {
            Action newAction = getRandomActionFromConfig();

            ongoingActions.add(newAction);
            lastAddedActionTime = LocalDateTime.now();
            announcement.announce("un nuovo task è stato richiesto, digita /ss tasks");
        }
    }

    // Keep action type, there are cases in which "RABBIT" matches both to a material and to an entity and we need to discriminate between the two
    private Action getActionFromString(String input, ActionType type){
        String[] splittedInput = input.split(":");

        int repetitions = Integer.parseInt(splittedInput[1]);
        int points = Integer.parseInt(splittedInput[2]);
        int expireMinutes = Integer.parseInt(splittedInput[3]);

        Material mat = null;
        try{
            mat = Material.matchMaterial(splittedInput[0].toUpperCase());
        } catch (IllegalArgumentException errorOk){}
        if(mat != null && ( type == null || type == ActionType.CRAFT)){
            return new CraftAction(points, repetitions, expireMinutes, config, mat);
        }

        Villager.Profession vill = null;
        try{
            vill = Villager.Profession.valueOf(splittedInput[0].toUpperCase());
        } catch (IllegalArgumentException errorOk){}
        if(vill != null && ( type == null || type == ActionType.TRADE)){
            return new TradeAction(points, repetitions, expireMinutes, config, vill);
        }

        EntityType ent = EntityType.valueOf(splittedInput[0].toUpperCase());
        if(ent != null && ( type == null || type == ActionType.KILL)){
            return new KillAction(points, repetitions, expireMinutes, config, ent);
        }

        logger.severe("Unable to instanciate actions for config: "+input);
        return null;
    }

    private Action getRandomActionFromConfig() {
        int actionsN = rawCraftingActions.size() + rawTradeActions.size() + rawKillingActions.size();
        int randomActionIndex = (new Random()).nextInt(actionsN);
        if (randomActionIndex < rawCraftingActions.size()) {
            return getActionFromString(rawCraftingActions.get(randomActionIndex), ActionType.CRAFT);
        }
        randomActionIndex = randomActionIndex - rawCraftingActions.size();
        if (randomActionIndex < rawTradeActions.size()) {
            return getActionFromString(rawTradeActions.get(randomActionIndex), ActionType.TRADE);
        }
        randomActionIndex = randomActionIndex - rawTradeActions.size();
        if (randomActionIndex < rawKillingActions.size()) {
            return getActionFromString(rawKillingActions.get(randomActionIndex), ActionType.KILL);
        }
        logger.severe("There is some error in getRandomActionFromConfig it should be mathematically impossible to reach this point : "+randomActionIndex+" "+rawTradeActions.size());
        return null;
    }

    public ComponentBuilder getTasksForPlayer(String player) {

        ComponentBuilder message = new ComponentBuilder("");
        message.append(new TextComponent(config.getString("current-tasks-prefix") + "\n"));

        for (Action action : ongoingActions) {
            message.append(action.getTaskDescription(player).create());
            message.append(new TextComponent("\n"));
        }
        return message;
    }

    private void addPlayerRepetition(Action cAction, Player player, int amount) {
        if (amount <= 0) // this usually happen on shift click but full inventory
            return;
        String playerName = player.getName();
        cAction.addPlayerRepetition(playerName, amount);

        if (cAction.playerHasAlreadyCompleted(playerName)) {
            announcement.announce(cAction.getSuccessMessage(playerName));
            scoreboardController.incrementScore(playerName, cAction.getPoints());
        } else {
            announcement.sendPrivateMessage(player, cAction.getProgressMessage(playerName));
        }
    }

    @EventHandler
    public void onPlayerCraftItem(CraftItemEvent event) {
        Player player = (Player) event.getWhoClicked();
        String playerName = player.getName();
        for (Action cAction : ongoingActions) {
            if (cAction.checkEvent(event) && !cAction.playerHasAlreadyCompleted(playerName)) {
                ClickType click = event.getClick();

                switch (click) {
                    case SHIFT_LEFT:
                    case SHIFT_RIGHT:
                        int previousItemInInventory = Arrays.stream(player.getInventory().getContents())
                                .filter(item -> item != null
                                        && item.getType() == event.getRecipe().getResult().getType())
                                .mapToInt(ItemStack::getAmount)
                                .sum();

                        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                            @Override
                            public void run() {
                                int successiveItemInInventory = Arrays.stream(player.getInventory().getContents())
                                        .filter(item -> item != null
                                                && item.getType() == event.getRecipe().getResult().getType())
                                        .mapToInt(ItemStack::getAmount)
                                        .sum();

                                addPlayerRepetition(cAction, player,
                                        successiveItemInInventory - previousItemInInventory);
                            }
                        });
                        break;

                    case NUMBER_KEY:
                        // If hotbar slot selected is full, crafting fails (vanilla behavior, even when
                        // items match)
                        if (player.getInventory().getItem(event.getHotbarButton()) == null)
                            addPlayerRepetition(cAction, player, event.getRecipe().getResult().getAmount());
                        break;

                    case DROP:
                    case CONTROL_DROP:
                        // If we are holding items, craft-via-drop fails (vanilla behavior)
                        // Apparently, rather than null, an empty cursor is AIR. I don't think that's
                        // intended.
                        if (event.getCursor().getType().isAir())
                            addPlayerRepetition(cAction, player, event.getRecipe().getResult().getAmount());
                        break;

                    default:
                        addPlayerRepetition(cAction, player, event.getRecipe().getResult().getAmount());
                        break;
                }
                return; // this return is important, if there are more equal task this return will make sure one at the time is being considered
            }
        }
    }

    @EventHandler
    public void onPlayerTradeWithVillager(PlayerTradeEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        for (Action cAction : ongoingActions) {
            if (cAction.checkEvent(event) && !cAction.playerHasAlreadyCompleted(playerName)) {
                addPlayerRepetition(cAction, player, 1);
                return; // this return is important, if there are more equal task this return will make sure one at the time is being considered
            }
        }
    }

    @EventHandler
    public void onPlayerKillZombie(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if(player == null)
            return;
        String playerName = player.getName();
        for (Action cAction : ongoingActions) {
            if (cAction.checkEvent(event) && !cAction.playerHasAlreadyCompleted(playerName)) {
                addPlayerRepetition(cAction, player, 1);
                return; // this return is important, if there are more equal task this return will make sure one at the time is being considered
            }
        }
    }
}
