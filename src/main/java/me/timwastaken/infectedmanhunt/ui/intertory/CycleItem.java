package me.timwastaken.infectedmanhunt.ui.intertory;

import me.timwastaken.intertoryapi.common.StringUtils;
import me.timwastaken.intertoryapi.inventories.items.ItemAction;
import me.timwastaken.intertoryapi.inventories.items.Items;
import me.timwastaken.intertoryapi.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.AbstractMap;
import java.util.List;

public class CycleItem extends Items.ItemWithState<Integer> {
    public CycleItem(
            int numStates,
            List<Material> stateMaterials,
            String title,
            List<String> descriptions
    ) {
        super(
                0,
                (state, type) -> {
                    int increment = 0;
                    if (type.isLeftClick()) increment = 1;
                    if (type.isRightClick()) increment = -1;
                    return new AbstractMap.SimpleEntry<>(
                            (state + increment) % numStates,
                            increment == 0 ? ItemAction.GENERIC_FAIL : (
                                    increment == 1 ? ItemAction.SMALL_INCREMENT
                                            : ItemAction.SMALL_DECREMENT
                                    )
                    );
                },
                (state) -> {
                    List<String> descriptionLore = StringUtils.wrapText(descriptions.get(state), 48)
                            .stream().map(
                                    line -> String.format("%s%s", ChatColor.GRAY, line)
                            ).toList();
                    Material type = stateMaterials.get(state);
                    return new ItemBuilder(type)
                            .name(title)
                            .addLoreLine("")
                            .addLoreLines(descriptionLore)
                            .addLoreLine("")
                            .addLoreLine(String.format(
                                    "%s%sOption %d/%d",
                                    ChatColor.DARK_GRAY,
                                    ChatColor.ITALIC,
                                    state + 1,
                                    numStates
                            ))
                            .build();
                }
        );
    }
}
