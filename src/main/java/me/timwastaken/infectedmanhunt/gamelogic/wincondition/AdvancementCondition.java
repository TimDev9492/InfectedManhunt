package me.timwastaken.infectedmanhunt.gamelogic.wincondition;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.function.Predicate;

public class AdvancementCondition extends WinCondition<PlayerAdvancementDoneEvent> {
    public AdvancementCondition(NamespacedKey advancementKey, Predicate<Player> runnerCheck) {
        super(event ->
                runnerCheck.test(event.getPlayer()) && event.getAdvancement().getKey().equals(advancementKey)
        );
    }

    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        if (!contextCheck.test(event)) return;
        if (callback == null) return;
        callback.run();
    }
}
