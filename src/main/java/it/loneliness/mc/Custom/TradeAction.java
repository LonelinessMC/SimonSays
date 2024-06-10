package it.loneliness.mc.Custom;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;

import de.unpixelt.locale.Locale;
import de.unpixelt.locale.Translate;
import io.papermc.paper.event.player.PlayerTradeEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;

class TradeAction extends Action {

    Villager.Profession professionToInteractWith;

    TradeAction(int points, int repetitions, int expireMinutes, FileConfiguration config,
            Villager.Profession professionToInteractWith) {
        super(points, repetitions, expireMinutes, config);
        this.professionToInteractWith = professionToInteractWith;
    }

    @Override
    boolean checkEvent(Event event) {
        if (!(event instanceof PlayerTradeEvent))
            return false;

        PlayerTradeEvent ptEvent = (PlayerTradeEvent) event;

        // Check if the entity interacted with is a villager
        if (ptEvent.getVillager().getType() == EntityType.VILLAGER) {
            AbstractVillager aVillager = ptEvent.getVillager();

            if (aVillager instanceof Villager) {
                Villager villager = (Villager) aVillager;

                if (villager.getProfession() == professionToInteractWith) {
                    return true;
                }

                // Check if the villager is a librarian
                if (villager.getProfession() == professionToInteractWith) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    ComponentBuilder getProgressMessage(String player) {
        return formatString2(player, this.config.getString("trade-progress"));
    }

    @Override
    ComponentBuilder getSuccessMessage(String player) {
        return formatString2(player, this.config.getString("trade-success"));
    }

    @Override
    ComponentBuilder getTaskDescription(String player) {
        if(this.playerHasAlreadyCompleted(player)){
            return formatString2(player, this.config.getString("trade-task-description-completed"));
        }else{
            return formatString2(player, this.config.getString("trade-task-description-ongoing"));
        }
    }

    ComponentBuilder formatString2(String player, String source){
        String output = "";

        String translatedName = Translate.getVillager(Locale.it_it, professionToInteractWith);

        int i = 0;
        String[] superPieces = super.formatString(player, source).split("\\{custom\\}");
        for (String piece : superPieces) {
            output += piece;
            if(i+1 < superPieces.length){
                output += translatedName != null ? translatedName : professionToInteractWith.name();
            }
            i++;
        }
        return new ComponentBuilder(output);
    }

}