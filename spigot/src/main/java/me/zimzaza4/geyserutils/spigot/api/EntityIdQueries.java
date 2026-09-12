package me.zimzaza4.geyserutils.spigot.api;

import me.zimzaza4.geyserutils.common.channel.GeyserUtilsChannels;
import me.zimzaza4.geyserutils.common.packet.EntityIdsRequestPacket;
import me.zimzaza4.geyserutils.common.packet.EntityIdsResponsePacket;
import me.zimzaza4.geyserutils.spigot.GeyserUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 面向单个连接的异步实体 ID 查询。仅保存两秒内的在途请求，不建立跨世界 ID 缓存。
 * 返回映射不含缺失实体；超时、断线或插件停用时异常完成。调用方不得在主线程等待 future。
 */
public final class EntityIdQueries {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final Map<Long, Pending> PENDING = new ConcurrentHashMap<>();

    private EntityIdQueries() {}

    public static CompletableFuture<Map<Integer, Long>> resolve(Player viewer, Collection<Integer> javaIds) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Entity ID queries must start on the server thread");
        Set<Integer> ids = Set.copyOf(javaIds);
        if (ids.size() > EntityIdsRequestPacket.MAX_ENTITIES) throw new IllegalArgumentException("Too many entity IDs");
        if (ids.isEmpty()) return CompletableFuture.completedFuture(Map.of());
        if (!viewer.isOnline()) return CompletableFuture.failedFuture(new IllegalStateException("Viewer is offline"));
        if (!viewer.getListeningPluginChannels().contains(GeyserUtilsChannels.ENTITY_IDS_CAPABILITY)) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("Entity ID query capability is unavailable"));
        }
        long id = SEQUENCE.incrementAndGet();
        CompletableFuture<Map<Integer, Long>> future = new CompletableFuture<>();
        Pending pending = new Pending(viewer.getUniqueId(), ids, future);
        PENDING.put(id, pending);
        future.orTimeout(2, TimeUnit.SECONDS).whenComplete((value, error) -> PENDING.remove(id, pending));
        try {
            viewer.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN,
                    GeyserUtils.getPacketManager().encodePacket(new EntityIdsRequestPacket(id, ids.stream().mapToInt(Integer::intValue).toArray())));
        } catch (RuntimeException error) {
            future.completeExceptionally(error);
        }
        return future;
    }

    public static void accept(Player viewer, EntityIdsResponsePacket packet) {
        Pending pending = PENDING.get(packet.getRequestId());
        if (pending == null || !pending.viewer.equals(viewer.getUniqueId())) return;
        int[] ids = packet.getJavaIds();
        long[] bedrockIds = packet.getBedrockIds();
        int[] missing = packet.getMissingIds();
        if (ids == null || bedrockIds == null || missing == null || ids.length != bedrockIds.length
                || ids.length + missing.length != pending.ids.size()) return;
        Map<Integer, Long> result = new HashMap<>();
        Set<Integer> accounted = new HashSet<>();
        for (int i = 0; i < ids.length; i++) {
            if (!pending.ids.contains(ids[i]) || !accounted.add(ids[i])) return;
            result.put(ids[i], bedrockIds[i]);
        }
        for (int id : missing) if (!pending.ids.contains(id) || !accounted.add(id)) return;
        pending.future.complete(Map.copyOf(result));
    }

    public static void cancelViewer(UUID viewer) {
        PENDING.values().stream().filter(p -> p.viewer.equals(viewer))
                .forEach(p -> p.future.completeExceptionally(new CancellationException("Viewer changed session")));
    }

    public static void shutdown() {
        PENDING.values().forEach(p -> p.future.completeExceptionally(new CancellationException("Plugin disabled")));
        PENDING.clear();
    }

    private record Pending(UUID viewer, Set<Integer> ids, CompletableFuture<Map<Integer, Long>> future) {}
}
