package me.zimzaza4.geyserutils.geyser;

import me.zimzaza4.geyserutils.common.packet.EntityIdsRequestPacket;
import me.zimzaza4.geyserutils.common.packet.EntityIdsResponsePacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** 所有查找在目标连接事件线程执行，使用 EntityCache 的本地玩家特殊映射；不缓存或推算 ID。 */
public final class EntityIdsQueryHandler {
    private EntityIdsQueryHandler() {}
    public static void handle(GeyserSession session, EntityIdsRequestPacket request, Consumer<EntityIdsResponsePacket> reply) {
        int[] requested = request.getJavaIds();
        if (requested == null || requested.length > EntityIdsRequestPacket.MAX_ENTITIES) return;
        int[] ids = requested.clone();
        session.ensureInEventLoop(() -> {
            Map<Integer, Long> found = new LinkedHashMap<>();
            Set<Integer> missing = new LinkedHashSet<>();
            for (int id : ids) {
                if (found.containsKey(id) || missing.contains(id)) continue;
                Entity entity = session.getEntityCache().getEntityByJavaId(id);
                if (entity == null || !entity.isValid()) missing.add(id);
                else found.put(id, entity.getGeyserId());
            }
            reply.accept(new EntityIdsResponsePacket(request.getRequestId(),
                    found.keySet().stream().mapToInt(Integer::intValue).toArray(),
                    found.values().stream().mapToLong(Long::longValue).toArray(),
                    missing.stream().mapToInt(Integer::intValue).toArray()));
        });
    }
}
