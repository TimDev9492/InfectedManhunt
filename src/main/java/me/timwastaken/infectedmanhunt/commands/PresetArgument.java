package me.timwastaken.infectedmanhunt.commands;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import me.timwastaken.infectedmanhunt.serialization.ConfigUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

public class PresetArgument extends ArgumentResolver<CommandSender, ConfigurationSection> {
    private final ConfigUtils presetLoader;

    public PresetArgument(ConfigUtils presetLoader) {
        this.presetLoader = presetLoader;
    }

    @Override
    protected ParseResult<ConfigurationSection> parse(
            Invocation<CommandSender> invocation,
            Argument<ConfigurationSection> context,
            String argument
    ) {
        ConfigurationSection section = presetLoader.getConfig(argument);
        if (section == null) return ParseResult.failure("Preset not found");
        return ParseResult.success(section);
    }

    @Override
    public SuggestionResult suggest(
            Invocation<CommandSender> invocation,
            Argument<ConfigurationSection> argument,
            SuggestionContext context
    ) {
        return presetLoader.listPresets().stream().collect(SuggestionResult.collector());
    }
}
