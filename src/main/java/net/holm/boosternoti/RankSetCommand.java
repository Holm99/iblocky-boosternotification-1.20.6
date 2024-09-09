package net.holm.boosternoti;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class RankSetCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("rankset")
                .then(literal("clear")  // Explicitly handle "clear" command first
                        .executes(context -> {
                            MinecraftClient client = MinecraftClient.getInstance();

                            if (client != null && client.player != null) {
                                UUID playerUUID = client.player.getUuid();
                                BoosterConfig config = BoosterConfig.load();

                                // Clear manual rank
                                config.clearManualRank(playerUUID);
                                client.player.sendMessage(Text.of("Manual rank cleared. Fetching rank from server..."), false);

                                // Immediately fetch and apply the rank from the server
                                iBlockyBoosterNotificationClient.getInstance().fetchAndLogPlayerPrefix();

                                return 1;
                            }
                            return 0;
                        }))
                .then(argument("rank", StringArgumentType.greedyString())  // Now handle "rank" argument separately
                        .suggests((context, builder) -> {
                            // Suggest ranks from the aliasMap, sorted alphabetically
                            Map<String, String> aliasMap = SellBoostCalculator.getAliasMap();
                            aliasMap.keySet().stream()
                                    .sorted()  // Sort alphabetically
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String rank = StringArgumentType.getString(context, "rank");
                            MinecraftClient client = MinecraftClient.getInstance();

                            if (client != null && client.player != null) {
                                UUID playerUUID = client.player.getUuid();

                                // Check only unformatted rank
                                Map<String, String> aliasMap = SellBoostCalculator.getAliasMap();
                                Map<String, Double> rankBoostMap = SellBoostCalculator.getRankBoostMap();

                                // Get formatted rank based on the alias
                                String formattedRank = aliasMap.getOrDefault(rank.toLowerCase(), rank);

                                // Ensure we are only checking the formatted rank from the alias map
                                if (rankBoostMap.containsKey(formattedRank)) {
                                    BoosterConfig config = BoosterConfig.load();
                                    config.setManualRank(playerUUID, formattedRank);

                                    // Set the rank
                                    SellBoostCalculator.setRank(formattedRank);
                                    client.player.sendMessage(Text.of("Rank set to: " + formattedRank), false);
                                    return 1;  // Success
                                } else {
                                    client.player.sendMessage(Text.of("Invalid rank: " + rank), false);
                                    return 0;  // Failure
                                }
                            }
                            return 0;
                        }))
        );
    }
}
