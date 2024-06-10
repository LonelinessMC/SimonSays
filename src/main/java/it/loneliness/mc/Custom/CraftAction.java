package it.loneliness.mc.Custom;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;

import de.unpixelt.locale.Locale;
import de.unpixelt.locale.Translate;
import net.md_5.bungee.api.chat.ComponentBuilder;

class CraftAction extends Action {

    Material materialToCraft;

    CraftAction(int points, int repetitions, int expireMinutes, FileConfiguration config,
            Material materialToCraft) {
        super(points, repetitions, expireMinutes, config);
        this.materialToCraft = materialToCraft;
    }

    @Override
    boolean checkEvent(Event event) {
        if (!(event instanceof CraftItemEvent))
            return false;

        CraftItemEvent cEvent = (CraftItemEvent) event;
        if (cEvent.getRecipe().getResult().getType() == this.materialToCraft) {
            return true;
        }

        return false;
    }

    @Override
    ComponentBuilder getProgressMessage(String player) {
        return formatString2(player, this.config.getString("craft-progress"));
    }

    @Override
    ComponentBuilder getSuccessMessage(String player) {
        return formatString2(player, this.config.getString("craft-success"));
    }

    @Override
    ComponentBuilder getTaskDescription(String player) {
        if(this.playerHasAlreadyCompleted(player)){
            return formatString2(player, this.config.getString("craft-task-description-completed"));
        }else{
            return formatString2(player, this.config.getString("craft-task-description-ongoing"));
        }
    }

    ComponentBuilder formatString2(String player, String source){
        String output = "";

        String originalName = materialToCraft.name().toLowerCase().replace("wall_", "");
        String translatedName = Translate.getCustomValue(materialToCraft.isBlock() ? "block.minecraft." + originalName : "item.minecraft." + originalName, Locale.it_it);

        int i = 0;
        String[] superPieces = super.formatString(player, source).split("\\{custom\\}");
        for (String piece : superPieces) {
            output+=piece;
            if(i+1 < superPieces.length){
                output += translatedName != null ? translatedName : originalName;
            }
            i++;
        }
        return new ComponentBuilder(output);
    }

}