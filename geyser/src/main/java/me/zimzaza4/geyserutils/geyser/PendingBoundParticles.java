package me.zimzaza4.geyserutils.geyser;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 绑定粒子的「延迟绑定，就绪即冲」（defer-and-flush）暂存区。
 *
 * <p>背景：SkyblockCore 在子弹出生那一 tick 就对「模型怪载体（BE = GeyserModelEngine 的 PIG）」发绑定粒子，
 * 但该载体的 spawn 被 GeyserModelEngine 压在 ≥50ms（首次进服可达 ~500ms）的异步延迟里，此刻
 * {@code session.getEntityCache().getEntityByJavaId(...)} 必然 miss。旧逻辑就地把「本该绑实体」的粒子降级成
 * 世界坐标 emitter —— 基岩粒子 fire-and-forget、无反向包，于是在出生点留下一圈永不跟随、活满
 * {@code active_time} 的「固定圆孤儿」。
 *
 * <p>修复：能观察 Geyser session 缓存真实状态的唯一位置就是本扩展。故：
 * <ul>
 *   <li>载体不在缓存 → {@link #park} 暂存，<b>绝不</b>回退世界坐标；</li>
 *   <li>载体进入缓存（{@code spawnEntity} 之后）→ {@link #flush} 冲刷该实体全部暂存、按 geyserId 绑定发送、各一次；</li>
 *   <li>载体始终不出现 → {@link #PARK_TTL_MS} 到期丢弃（fail-closed，绝不产孤儿）；</li>
 *   <li>{@link #markTombstone 墓碑}（javaId+identifier，短 TTL）去重 SkyblockCore 的 tick+2 rebind 二次发送，保证「精确一次」。</li>
 * </ul>
 *
 * <p>对所有 {@code bedrockparticle{bind=…}}（怪物 / 武器 / 未来）永久生效，服务端零改动、无逐招特调。
 *
 * <p>线程：park / flush / sendBoundOnce 均在 session 下行（Netty）线程调用，彼此无竞争；仅 TTL 清扫在
 * {@link GeyserUtils#scheduler} 线程跨线程访问，故全程用并发容器 + 幂等移除。
 */
public final class PendingBoundParticles {

    private PendingBoundParticles() {
    }

    /** 暂存粒子存活上限：覆盖首次进服最坏 ~500ms 的载体延迟 + 抖动；到期即 fail-closed 丢弃（绝不世界坐标兜底）。 */
    private static final long PARK_TTL_MS = 1500;
    /** 墓碑存活：只需跨越「flush 早于随后到达的 tick+2 rebind」的空档（rebind 固定 ~100ms），500ms 足量且不误杀正常重发。 */
    private static final long TOMBSTONE_TTL_MS = 500;

    private static final class Parked {
        final SpawnParticleEffectPacket packet;
        volatile ScheduledFuture<?> ttl;

        Parked(SpawnParticleEffectPacket packet) {
            this.packet = packet;
        }
    }

    private static final class State {
        /** javaId -> (identifier -> 暂存包)。内层按 identifier 去重，防同一实体同一粒子重复暂存。 */
        final Map<Integer, Map<String, Parked>> pending = new ConcurrentHashMap<>();
        /** "javaId\0identifier"：近期已发送过的绑定粒子墓碑，用于去重 rebind。 */
        final Map<String, Boolean> tombstones = new ConcurrentHashMap<>();
    }

    private static final Map<GeyserSession, State> STATES = new ConcurrentHashMap<>();

    private static State state(GeyserSession session) {
        return STATES.computeIfAbsent(session, s -> new State());
    }

    private static String key(int javaId, String identifier) {
        return javaId + "\0" + identifier;
    }

    /**
     * 载体尚未进 session 缓存 → 暂存该绑定粒子，等 {@link #flush}。
     * 已有同键墓碑（flush 已发过）或已在暂存 → 直接丢弃（去重）。
     */
    public static void park(GeyserSession session, int javaId, String identifier, SpawnParticleEffectPacket packet) {
        State st = state(session);
        if (st.tombstones.containsKey(key(javaId, identifier))) {
            GeyserImpl.getInstance().getLogger().debug("[GeyserUtils] particle '" + identifier
                    + "' park javaId=" + javaId + " suppressed (already flushed this window)");
            return;
        }
        Parked parked = new Parked(packet);
        Map<String, Parked> byId = st.pending.computeIfAbsent(javaId, id -> new ConcurrentHashMap<>());
        if (byId.putIfAbsent(identifier, parked) != null) {
            GeyserImpl.getInstance().getLogger().debug("[GeyserUtils] particle '" + identifier
                    + "' park javaId=" + javaId + " deduped (already pending)");
            return;
        }
        parked.ttl = GeyserUtils.scheduler.schedule(() -> {
            Map<String, Parked> m = st.pending.get(javaId);
            if (m != null && m.remove(identifier, parked)) {
                if (m.isEmpty()) st.pending.remove(javaId, m);
                GeyserImpl.getInstance().getLogger().debug("[GeyserUtils] particle '" + identifier
                        + "' park javaId=" + javaId + " dropped (carrier never appeared, fail-closed)");
            }
        }, PARK_TTL_MS, TimeUnit.MILLISECONDS);
        GeyserImpl.getInstance().getLogger().debug("[GeyserUtils] particle '" + identifier
                + "' parked javaId=" + javaId + " (awaiting carrier spawn)");
    }

    /**
     * 载体的 spawn 已发生（{@code spawnEntity} 之后）→ 冲刷该 javaId 的全部暂存绑定粒子，各发一次并登记墓碑。
     * 对无暂存的普通实体 spawn 是廉价 no-op。
     */
    public static void flush(GeyserSession session, int javaId, long geyserId) {
        State st = STATES.get(session);
        if (st == null) return;
        Map<String, Parked> byId = st.pending.remove(javaId);
        if (byId == null || byId.isEmpty()) return;
        for (Map.Entry<String, Parked> e : byId.entrySet()) {
            Parked parked = e.getValue();
            if (parked.ttl != null) parked.ttl.cancel(false);
            sendBound(session, parked.packet, geyserId);
            markTombstone(st, javaId, e.getKey());
            GeyserImpl.getInstance().getLogger().debug("[GeyserUtils] particle '" + e.getKey()
                    + "' flushed javaId=" + javaId + " -> geyserId=" + geyserId);
        }
    }

    /**
     * 载体已在缓存 → 立即绑定发送，除非该 (javaId, identifier) 刚被发过（墓碑命中 → 抑制，去重 rebind）。
     *
     * @return true = 已发送；false = 被墓碑抑制。
     */
    public static boolean sendBoundOnce(GeyserSession session, int javaId, String identifier,
                                        SpawnParticleEffectPacket packet, long geyserId) {
        State st = state(session);
        if (st.tombstones.containsKey(key(javaId, identifier))) {
            return false;
        }
        sendBound(session, packet, geyserId);
        markTombstone(st, javaId, identifier);
        return true;
    }

    /** session 断开：清空其 pending / 墓碑，取消未决 TTL。 */
    public static void clear(GeyserSession session) {
        State st = STATES.remove(session);
        if (st == null) return;
        for (Map<String, Parked> byId : st.pending.values()) {
            for (Parked parked : byId.values()) {
                if (parked.ttl != null) parked.ttl.cancel(false);
            }
        }
        st.pending.clear();
        st.tombstones.clear();
    }

    private static void sendBound(GeyserSession session, SpawnParticleEffectPacket packet, long geyserId) {
        packet.setUniqueEntityId(geyserId);
        // position 相对实体，置 ZERO（配合 particle.json 的 minecraft:emitter_local_space）。
        packet.setPosition(Vector3f.ZERO);
        session.sendUpstreamPacket(packet);
    }

    private static void markTombstone(State st, int javaId, String identifier) {
        String k = key(javaId, identifier);
        st.tombstones.put(k, Boolean.TRUE);
        GeyserUtils.scheduler.schedule(() -> st.tombstones.remove(k), TOMBSTONE_TTL_MS, TimeUnit.MILLISECONDS);
    }
}
