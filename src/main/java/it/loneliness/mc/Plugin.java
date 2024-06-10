package it.loneliness.mc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

import it.loneliness.mc.Controller.CommandHandler;
import it.loneliness.mc.Controller.TaskScheduler;
import it.loneliness.mc.Custom.ActionsManager;
import it.loneliness.mc.Model.LogHandler;

public class Plugin extends JavaPlugin{
    LogHandler logger;
    CommandHandler commandHandler;
    TaskScheduler taskScheduler;
    
    @Override
    public void onEnable() {
        logger = LogHandler.getInstance(getLogger());
        logger.info("Enabling the plugin");

        if (!checkAndLoadConfig()) {
            logger.severe("Configuration is invalid. Disabling the plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if(getConfig().getBoolean("debug")){
            logger.setDebug(true);
        }

        //Make sure this is alligned with the plugin.yml, the first in the list is used for the permissions
        List<String> prefixes = new ArrayList<>(Arrays.asList("simonsays", "ss"));
        this.commandHandler = new CommandHandler(this, prefixes);
        for(String prefix : prefixes){
            this.getCommand(prefix).setExecutor(commandHandler);
            this.getCommand(prefix).setTabCompleter(commandHandler);
        }
        
        
        //PLUGIN SPECIFIC IMPLEMENTATION
        ActionsManager actionsManager = ActionsManager.getInstance(this);
        getServer().getPluginManager().registerEvents(actionsManager, this);
        //END PLUGIN SPECIFIC IMPLEMENTATION


        this.taskScheduler = new TaskScheduler(this, 60, actionsManager); //in seconds
        taskScheduler.start();
    }

    @Override
    public void onDisable() {
        logger.info("Disabling the plugin");
        taskScheduler.stop();
    }

    /**
     * Checks and loads the configuration file.
     * @return true if the configuration is valid, false otherwise.
     */
    private boolean checkAndLoadConfig() {
        // Save the default config if it does not exist
        saveDefaultConfig();

        String scoreboardId = getConfig().getString("scoreboard-id");
        if (scoreboardId.isBlank() || scoreboardId.isEmpty()) {
            logger.severe("No scoreboard id specified in the config!");
            return false;
        }

        List<String> craftActions = getConfig().getStringList("craft-actions");
        for (String action : craftActions) {
            Material material = Material.matchMaterial(action.split(":")[0]);
            if(material == null){
                logger.severe("No material "+action.split(":")[0]+" for :"+action+"!");
                return false; 
            }
            int repetitions = Integer.parseInt(action.split(":")[1]);
            if(repetitions < 0){
                logger.severe("Invalid number of repetitions for :"+action+"!");
                return false; 
            }
            int points = Integer.parseInt(action.split(":")[2]);
            if(points < 0){
                logger.severe("Invalid number of points for :"+action+"!");
                return false; 
            }
            int expireMinutes = Integer.parseInt(action.split(":")[3]);
            if(expireMinutes < 0){
                logger.severe("Invalid number of expireMinutes for :"+action+"!");
                return false; 
            }
        }

        List<String> tradeActions = getConfig().getStringList("trade-actions");
        for (String action : tradeActions) {
            Villager.Profession vp = Villager.Profession.valueOf(action.split(":")[0].toUpperCase());
            if(vp == null){
                logger.severe("No villager profession "+action.split(":")[0]+" for :"+action+"!");
                return false; 
            }
            int repetitions = Integer.parseInt(action.split(":")[1]);
            if(repetitions < 0){
                logger.severe("Invalid number of repetitions for :"+action+"!");
                return false; 
            }
            int points = Integer.parseInt(action.split(":")[2]);
            if(points < 0){
                logger.severe("Invalid number of points for :"+action+"!");
                return false; 
            }
            int expireMinutes = Integer.parseInt(action.split(":")[3]);
            if(expireMinutes < 0){
                logger.severe("Invalid number of expireMinutes for :"+action+"!");
                return false; 
            }
        }

        List<String> killActions = getConfig().getStringList("kill-actions");
        for (String action : killActions) {
            EntityType vp = EntityType.valueOf(action.split(":")[0].toUpperCase());
            if(vp == null){
                logger.severe("No enity type "+action.split(":")[0]+" for :"+action+"!");
                return false; 
            }
            int repetitions = Integer.parseInt(action.split(":")[1]);
            if(repetitions < 0){
                logger.severe("Invalid number of repetitions for :"+action+"!");
                return false; 
            }
            int points = Integer.parseInt(action.split(":")[2]);
            if(points < 0){
                logger.severe("Invalid number of points for :"+action+"!");
                return false; 
            }
            int expireMinutes = Integer.parseInt(action.split(":")[3]);
            if(expireMinutes < 0){
                logger.severe("Invalid number of expireMinutes for :"+action+"!");
                return false; 
            }
        }
        // this.rawKillingActions = getConfig().getStringList("kill-actions");

        // Log the loaded prefixes for debugging
        return true;
    }

    public TaskScheduler getTaskScheduler(){
        return this.taskScheduler;
    }
}