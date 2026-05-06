package com.minhduong.command;

import com.minhduong.data.CommandToggleManager;
import com.minhduong.util.Messages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandToggleCommand {
    private static final Permission COMMAND_PERMISSION =
            new Permission.Level(PermissionLevel.GAMEMASTERS);

    private static final SuggestionProvider<ServerCommandSource> SUPPORTED_COMMANDS =
            (ctx, builder) -> CommandSource.suggestMatching(
                    CommandToggleManager.getSupportedCommands(),
                    builder
            );

    private static final SuggestionProvider<ServerCommandSource> DISABLED_COMMANDS =
            (ctx, builder) -> CommandSource.suggestMatching(
                    CommandToggleManager.getDisabledCommands(),
                    builder
            );

    private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS =
            (ctx, builder) -> CommandSource.suggestMatching(
                    ctx.getSource().getServer().getPlayerManager().getPlayerList().stream()
                            .map(p -> p.getName().getString())
                            .toList(),
                    builder
            );

    private static final SuggestionProvider<ServerCommandSource> ALLOWED_PLAYERS_FOR_COMMAND =
            (ctx, builder) -> {
                String commandName = StringArgumentType.getString(ctx, "name");
                return CommandSource.suggestMatching(
                        CommandToggleManager.getAllowedPlayers(commandName),
                        builder
                );
            };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tryagain")
                .then(CommandManager.literal("command")
                        .requires(CommandToggleCommand::hasPermission)
                        .then(CommandManager.literal("disable")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .suggests(SUPPORTED_COMMANDS)
                                        .executes(ctx -> disableCommand(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name")
                                        ))))
                        .then(CommandManager.literal("enable")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .suggests(DISABLED_COMMANDS)
                                        .executes(ctx -> enableCommand(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name")
                                        ))))
                        .then(CommandManager.literal("allow")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .suggests(DISABLED_COMMANDS)
                                        .then(CommandManager.argument("player", StringArgumentType.word())
                                                .suggests(ONLINE_PLAYERS)
                                                .executes(ctx -> allowPlayer(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        StringArgumentType.getString(ctx, "player")
                                                )))))
                        .then(CommandManager.literal("unallow")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .suggests(SUPPORTED_COMMANDS)
                                        .then(CommandManager.argument("player", StringArgumentType.word())
                                                .suggests(ALLOWED_PLAYERS_FOR_COMMAND)
                                                .executes(ctx -> unallowPlayer(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        StringArgumentType.getString(ctx, "player")
                                                )))))
                        .then(CommandManager.literal("list")
                                .executes(ctx -> listCommands(ctx.getSource())))
                ));
    }

    private static int disableCommand(ServerCommandSource source, String rawName) {
        if (!hasPermission(source)) {
            source.sendMessage(Messages.error("Ban khong co quyen dung lenh nay."));
            return 0;
        }

        String commandName = rawName.toLowerCase(Locale.ROOT);

        if (!CommandToggleManager.isSupported(commandName)) {
            source.sendMessage(Messages.error("Lenh /" + rawName + " khong ho tro tat/bat."));
            return 0;
        }

        if (CommandToggleManager.isDisabled(commandName)) {
            source.sendMessage(Messages.info("Lenh /" + commandName + " da dang bi tat."));
            return 0;
        }

        CommandToggleManager.disableCommand(commandName);
        source.sendMessage(Messages.success("Da tat lenh /" + commandName + "."));
        return 1;
    }

    private static int enableCommand(ServerCommandSource source, String rawName) {
        if (!hasPermission(source)) {
            source.sendMessage(Messages.error("Ban khong co quyen dung lenh nay."));
            return 0;
        }

        String commandName = rawName.toLowerCase(Locale.ROOT);

        if (!CommandToggleManager.isSupported(commandName)) {
            source.sendMessage(Messages.error("Lenh /" + rawName + " khong ho tro tat/bat."));
            return 0;
        }

        if (!CommandToggleManager.isDisabled(commandName)) {
            source.sendMessage(Messages.info("Lenh /" + commandName + " hien dang bat."));
            return 0;
        }

        CommandToggleManager.enableCommand(commandName);
        source.sendMessage(Messages.success("Da bat lai lenh /" + commandName + "."));
        return 1;
    }

    private static int allowPlayer(ServerCommandSource source, String rawCommandName, String rawPlayerName) {
        if (!hasPermission(source)) {
            source.sendMessage(Messages.error("Ban khong co quyen dung lenh nay."));
            return 0;
        }

        String commandName = rawCommandName.toLowerCase(Locale.ROOT);
        String playerName = rawPlayerName.toLowerCase(Locale.ROOT);

        if (!CommandToggleManager.isSupported(commandName)) {
            source.sendMessage(Messages.error("Lenh /" + rawCommandName + " khong ho tro tat/bat."));
            return 0;
        }

        if (!CommandToggleManager.isDisabled(commandName)) {
            source.sendMessage(Messages.info("Lenh /" + commandName + " dang bat. Hay disable truoc."));
            return 0;
        }

        if (!CommandToggleManager.allowPlayer(commandName, playerName)) {
            source.sendMessage(Messages.info("Player " + playerName + " da duoc phep dung /" + commandName + "."));
            return 0;
        }

        source.sendMessage(Messages.success("Da cho phep " + playerName + " dung /" + commandName + "."));
        return 1;
    }

    private static int unallowPlayer(ServerCommandSource source, String rawCommandName, String rawPlayerName) {
        if (!hasPermission(source)) {
            source.sendMessage(Messages.error("Ban khong co quyen dung lenh nay."));
            return 0;
        }

        String commandName = rawCommandName.toLowerCase(Locale.ROOT);
        String playerName = rawPlayerName.toLowerCase(Locale.ROOT);

        if (!CommandToggleManager.isSupported(commandName)) {
            source.sendMessage(Messages.error("Lenh /" + rawCommandName + " khong ho tro tat/bat."));
            return 0;
        }

        if (!CommandToggleManager.unallowPlayer(commandName, playerName)) {
            source.sendMessage(Messages.info("Player " + playerName + " chua duoc allow cho /" + commandName + "."));
            return 0;
        }

        source.sendMessage(Messages.success("Da go allow " + playerName + " khoi /" + commandName + "."));
        return 1;
    }

    private static int listCommands(ServerCommandSource source) {
        if (!hasPermission(source)) {
            source.sendMessage(Messages.error("Ban khong co quyen dung lenh nay."));
            return 0;
        }

        List<String> supported = CommandToggleManager.getSupportedCommands();
        List<String> disabled = CommandToggleManager.getDisabledCommands();

        source.sendMessage(Messages.info("Lenh co the quan ly: /" + String.join(", /", supported)));

        if (disabled.isEmpty()) {
            source.sendMessage(Messages.info("Khong co lenh nao dang bi tat."));
            return 1;
        }

        source.sendMessage(Messages.info("Lenh dang bi tat: /" + String.join(", /", disabled)));

        Map<String, List<String>> overrides = CommandToggleManager.getAllowedPlayersByCommand();
        for (String commandName : disabled) {
            List<String> players = overrides.get(commandName);
            if (players == null || players.isEmpty()) {
                source.sendMessage(Messages.info("/" + commandName + " -> khong co player nao duoc allow."));
                continue;
            }
            source.sendMessage(Messages.info("/" + commandName + " -> allow: " + String.join(", ", players)));
        }
        return 1;
    }

    private static boolean hasPermission(ServerCommandSource source) {
        return source.getPermissions().hasPermission(COMMAND_PERMISSION);
    }
}
