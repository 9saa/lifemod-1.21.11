package com.example.lifemod.network;

import com.example.lifemod.LifeMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ModNetworking {

    public record PlayerEntry(UUID uuid, String name, int lives) {}

    // =========================================================
    // S2C: open the revive beacon screen
    // =========================================================
    public record OpenReviveScreenPayload(BlockPos beaconPos, String encodedPlayers)
            implements CustomPayload {
        public static final Identifier ID = Identifier.of(LifeMod.MOD_ID, "open_revive_screen");
        public static final CustomPayload.Id<OpenReviveScreenPayload> PACKET_ID =
                new CustomPayload.Id<>(ID);

        public static final PacketCodec<PacketByteBuf, OpenReviveScreenPayload> CODEC =
                PacketCodec.tuple(
                        BlockPos.PACKET_CODEC, OpenReviveScreenPayload::beaconPos,
                        PacketCodecs.STRING, OpenReviveScreenPayload::encodedPlayers,
                        OpenReviveScreenPayload::new
                );

        public List<PlayerEntry> players() {
            List<PlayerEntry> result = new ArrayList<>();
            if (encodedPlayers == null || encodedPlayers.isEmpty()) return result;
            for (String part : encodedPlayers.split(";")) {
                if (part.isEmpty()) continue;
                String[] f = part.split("\\|", 3);
                if (f.length != 3) continue;
                try {
                    result.add(new PlayerEntry(
                            UUID.fromString(f[0]), f[1], Integer.parseInt(f[2])));
                } catch (Exception ignored) {}
            }
            return result;
        }

        public static OpenReviveScreenPayload from(BlockPos pos, List<PlayerEntry> players) {
            StringBuilder sb = new StringBuilder();
            for (PlayerEntry p : players) {
                if (sb.length() > 0) sb.append(";");
                sb.append(p.uuid()).append("|").append(p.name()).append("|").append(p.lives());
            }
            return new OpenReviveScreenPayload(pos, sb.toString());
        }

        @Override
        public Id<? extends CustomPayload> getId() { return PACKET_ID; }
    }

    // =========================================================
    // S2C: YOUR OWN lives (for the HUD hearts above the hotbar)
    // =========================================================
    public record LifeDataPayload(int lives) implements CustomPayload {
        public static final Identifier ID = Identifier.of(LifeMod.MOD_ID, "life_data");
        public static final CustomPayload.Id<LifeDataPayload> PACKET_ID =
                new CustomPayload.Id<>(ID);

        public static final PacketCodec<PacketByteBuf, LifeDataPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.VAR_INT, LifeDataPayload::lives,
                        LifeDataPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() { return PACKET_ID; }
    }

    // =========================================================
    // S2C: EVERYONE'S lives (for the tab list)
    // =========================================================
    public record AllLivesPayload(String encoded) implements CustomPayload {
        public static final Identifier ID = Identifier.of(LifeMod.MOD_ID, "all_lives");
        public static final CustomPayload.Id<AllLivesPayload> PACKET_ID =
                new CustomPayload.Id<>(ID);

        public static final PacketCodec<PacketByteBuf, AllLivesPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, AllLivesPayload::encoded,
                        AllLivesPayload::new
                );

        public Map<UUID, Integer> decode() {
            Map<UUID, Integer> map = new HashMap<>();
            if (encoded == null || encoded.isEmpty()) return map;
            for (String part : encoded.split(";")) {
                if (part.isEmpty()) continue;
                String[] f = part.split("\\|", 2);
                if (f.length != 2) continue;
                try {
                    map.put(UUID.fromString(f[0]), Integer.parseInt(f[1]));
                } catch (Exception ignored) {}
            }
            return map;
        }

        public static AllLivesPayload from(Map<UUID, Integer> map) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<UUID, Integer> e : map.entrySet()) {
                if (sb.length() > 0) sb.append(";");
                sb.append(e.getKey()).append("|").append(e.getValue());
            }
            return new AllLivesPayload(sb.toString());
        }

        @Override
        public Id<? extends CustomPayload> getId() { return PACKET_ID; }
    }

    // =========================================================
    // C2S: revive request from the beacon GUI
    // =========================================================
    public record ReviveRequestPayload(UUID targetUuid, BlockPos beaconPos)
            implements CustomPayload {
        public static final Identifier ID = Identifier.of(LifeMod.MOD_ID, "revive_request");
        public static final CustomPayload.Id<ReviveRequestPayload> PACKET_ID =
                new CustomPayload.Id<>(ID);

        public static final PacketCodec<PacketByteBuf, ReviveRequestPayload> CODEC =
                PacketCodec.tuple(
                        Uuids.PACKET_CODEC, ReviveRequestPayload::targetUuid,
                        BlockPos.PACKET_CODEC, ReviveRequestPayload::beaconPos,
                        ReviveRequestPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() { return PACKET_ID; }
    }

    // =========================================================
    // REGISTRATION — called from LifeMod.onInitialize()
    // =========================================================
    public static void register() {
        // --- S2C ---
        PayloadTypeRegistry.playS2C().register(
                OpenReviveScreenPayload.PACKET_ID, OpenReviveScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                LifeDataPayload.PACKET_ID, LifeDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                AllLivesPayload.PACKET_ID, AllLivesPayload.CODEC);

        // --- C2S ---
        PayloadTypeRegistry.playC2S().register(
                ReviveRequestPayload.PACKET_ID, ReviveRequestPayload.CODEC);

        // --- Server-side receivers ---
        ServerPlayNetworking.registerGlobalReceiver(
                ReviveRequestPayload.PACKET_ID,
                (payload, context) -> ReviveHandler.handle(
                        context.player(), payload.targetUuid(), payload.beaconPos()));
    }

    // =========================================================
    // Convenience senders
    // =========================================================
    public static void sendAllLivesToAll(MinecraftServer server) {
        Map<UUID, Integer> map = new HashMap<>();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            var data = com.example.lifemod.data.LifeState.getPlayerData(server, p);
            if (data != null) map.put(p.getUuid(), data.getLives());
        }

        // --- DEBUG ---
        System.out.println("[LIFEMOD-DEBUG] sendAllLivesToAll: map=" + map
                + " players=" + server.getPlayerManager().getPlayerList().size());

        AllLivesPayload payload = AllLivesPayload.from(map);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }
}
