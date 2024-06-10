package it.loneliness.mc.Custom;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

import de.unpixelt.locale.Locale;
import de.unpixelt.locale.Translate;
import net.md_5.bungee.api.chat.ComponentBuilder;

class KillAction extends Action {

    EntityType entityToKill;

    KillAction(int points, int repetitions, int expireMinutes, FileConfiguration config, EntityType entityToKill) {
        super(points, repetitions, expireMinutes, config);
        this.entityToKill = entityToKill;
    }

    @Override
    boolean checkEvent(Event event) {
        if (!(event instanceof EntityDeathEvent))
            return false;

        EntityDeathEvent cEvent = (EntityDeathEvent) event;
        if (cEvent.getEntityType() == entityToKill && cEvent.getEntity().getKiller() != null) {
            return true;
        }

        return false;
    }

    @Override
    ComponentBuilder getProgressMessage(String player) {
        return formatString2(player, this.config.getString("kill-progress"));
    }

    @Override
    ComponentBuilder getSuccessMessage(String player) {
        return formatString2(player, this.config.getString("kill-success"));
    }

    @Override
    ComponentBuilder getTaskDescription(String player) {
        if(this.playerHasAlreadyCompleted(player)){
            return formatString2(player, this.config.getString("kill-task-description-completed"));
        }else{
            return formatString2(player, this.config.getString("kill-task-description-ongoing"));
        }
    }

    ComponentBuilder formatString2(String player, String source){
        String output = "";

        String translatedName = Translate.getEntity(Locale.it_it, entityToKill);

        int i = 0;
        String[] superPieces = super.formatString(player, source).split("\\{custom\\}");
        for (String piece : superPieces) {
            output+=piece;
            if(i+1 < superPieces.length){
                output+=translatedName != null ? translatedName : entityToKill.name();
            }
            i++;
        }
        return new ComponentBuilder(output);
    }

}