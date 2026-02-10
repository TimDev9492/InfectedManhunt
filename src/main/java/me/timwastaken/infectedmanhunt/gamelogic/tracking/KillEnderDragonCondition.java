package me.timwastaken.infectedmanhunt.gamelogic.tracking;

import me.timwastaken.infectedmanhunt.gamelogic.WinCondition;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

public class KillEnderDragonCondition extends WinCondition<EntityDeathEvent> {
    public KillEnderDragonCondition() {
        super(event -> event.getEntityType().equals(EntityType.ENDER_DRAGON));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!contextCheck.test(event)) return;
        if (callback == null) return;
        callback.run();
    }
}
