package me.timwastaken.infectedmanhunt.ui.intertory;

import me.timwastaken.intertoryapi.common.StringUtils;
import me.timwastaken.intertoryapi.inventories.items.ItemAction;
import me.timwastaken.intertoryapi.inventories.items.Items;
import me.timwastaken.intertoryapi.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.AbstractMap;
import java.util.List;

public class ToggleWithActionItem extends Items.ItemWithState<Boolean> {
    public ToggleWithActionItem(
            Material activeType,
            Material inactiveType,
            String activeTitle,
            String inactiveTitle,
            String activeDesc,
            String inactiveDesc,
            Runnable onEnable,
            Runnable onDisable,
            boolean enabled
    ) {
        super(
                enabled,
                (isEnabled, type) -> {
                    (isEnabled ? onDisable : onEnable).run();
                    return new AbstractMap.SimpleEntry<>(
                            !isEnabled,
                            !isEnabled ? ItemAction.TOGGLE_ENABLED : ItemAction.TOGGLE_DISABLED
                    );
                },
                (isEnabled) -> {
                    List<String> descriptionLore = StringUtils
                            .wrapText(isEnabled ? activeDesc : inactiveDesc, 48)
                            .stream().map(
                                    line -> String.format("%s%s", ChatColor.GRAY, line)
                            ).toList();
                    return new ItemBuilder(isEnabled ? activeType : inactiveType)
                            .name(isEnabled ? activeTitle : inactiveTitle)
                            .addLoreLine("")
                            .addLoreLines(descriptionLore)
                            .build();
                }
        );
    }
}
