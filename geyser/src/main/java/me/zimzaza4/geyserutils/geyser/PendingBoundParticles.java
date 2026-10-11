package me.zimzaza4.geyserutils.geyser;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;

/**
 * Briefly waits for a requested particle carrier to spawn. Each request is a separate playback,
 * including repeated identifiers and identical Molang values. Missing carriers expire without
 * creating a world-position emitter. All per-session state and timers run on that session's loop.
 */
public final class PendingBoundParticles {
    private static final long PARK_TTL_MS = 1500;
    private static final Map<GeyserSession, State> STATES = new ConcurrentHashMap<>();

    private static final class Parked {
        final SpawnParticleEffectPacket packet;
        ScheduledFuture<?> ttl;
        Parked(SpawnParticleEffectPacket packet) { this.packet = packet; }
    }

    private static final class State {
        final Map<Integer, ArrayDeque<Parked>> pending = new HashMap<>();
    }

    private PendingBoundParticles() { }

    /** Sends a distinct playback now, or waits for its explicitly requested carrier. */
    public static void send(GeyserSession session, int javaId, SpawnParticleEffectPacket packet) {
        session.ensureInEventLoop(() -> {
            if (session.isClosed()) return;
            Entity entity = session.getEntityCache().getEntityByJavaId(javaId);
            if (entity != null && entity.isValid()) {
                sendBound(session, packet, entity.getGeyserId());
            } else {
                park(session, javaId, packet);
            }
        });
    }

    private static void park(GeyserSession session, int javaId, SpawnParticleEffectPacket packet) {
        State state = STATES.computeIfAbsent(session, ignored -> new State());
        ArrayDeque<Parked> queue = state.pending.computeIfAbsent(javaId, ignored -> new ArrayDeque<>());
        Parked parked = new Parked(packet);
        queue.addLast(parked);
        parked.ttl = session.scheduleInEventLoop(() -> {
            if (queue.remove(parked) && queue.isEmpty()) {
                state.pending.remove(javaId, queue);
                if (state.pending.isEmpty()) STATES.remove(session, state);
            }
        }, PARK_TTL_MS, TimeUnit.MILLISECONDS);
    }

    /** Called after successful spawn; each still-pending request is removed before sending. */
    public static void flush(GeyserSession session, int javaId, long geyserId) {
        session.ensureInEventLoop(() -> {
            State state = STATES.get(session);
            if (state == null) return;
            ArrayDeque<Parked> queue = state.pending.remove(javaId);
            if (state.pending.isEmpty()) STATES.remove(session, state);
            if (queue == null) return;
            for (Parked parked : queue) {
                parked.ttl.cancel(false);
                if (!session.isClosed()) sendBound(session, parked.packet, geyserId);
            }
            queue.clear();
        });
    }

    /** Cancels queued requests on disconnect; no expiry callback can revive them. */
    public static void clear(GeyserSession session) {
        session.ensureInEventLoop(() -> {
            State state = STATES.remove(session);
            if (state == null) return;
            for (ArrayDeque<Parked> queue : state.pending.values()) {
                for (Parked parked : queue) parked.ttl.cancel(false);
                queue.clear();
            }
            state.pending.clear();
        });
    }

    private static void sendBound(GeyserSession session, SpawnParticleEffectPacket packet, long geyserId) {
        packet.setUniqueEntityId(geyserId);
        packet.setPosition(Vector3f.ZERO);
        session.sendUpstreamPacket(packet);
    }
}
