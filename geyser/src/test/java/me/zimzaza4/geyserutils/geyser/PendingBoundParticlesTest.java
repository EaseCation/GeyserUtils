package me.zimzaza4.geyserutils.geyser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.SpawnParticleEffectPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.EntityCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PendingBoundParticlesTest {
    private GeyserSession session;
    private EntityCache cache;
    private final List<Runnable> expiries = new ArrayList<>();
    private final List<ScheduledFuture<?>> timers = new ArrayList<>();
    private final List<SpawnParticleEffectPacket> sent = new ArrayList<>();

    @BeforeEach void setup() {
        session = mock(GeyserSession.class);
        cache = mock(EntityCache.class);
        when(session.getEntityCache()).thenReturn(cache);
        doAnswer(call -> { call.<Runnable>getArgument(0).run(); return null; })
                .when(session).ensureInEventLoop(any());
        when(session.scheduleInEventLoop(any(), eq(1500L), eq(TimeUnit.MILLISECONDS))).thenAnswer(call -> {
            expiries.add(call.getArgument(0));
            ScheduledFuture<?> timer = mock(ScheduledFuture.class);
            timers.add(timer);
            return timer;
        });
        doAnswer(call -> { sent.add(call.getArgument(0)); return null; })
                .when(session).sendUpstreamPacket(any(SpawnParticleEffectPacket.class));
    }

    @AfterEach void cleanup() { PendingBoundParticles.clear(session); }

    private SpawnParticleEffectPacket particle(String vars) {
        var packet = new SpawnParticleEffectPacket();
        packet.setIdentifier("test:same");
        packet.setMolangVariablesJson(Optional.of(vars));
        packet.setPosition(Vector3f.from(7, 8, 9));
        return packet;
    }

    @Test void retainsEveryPlaybackAndItsMolangUntilCarrierSpawns() {
        var first = particle("first");
        var second = particle("second");
        var third = particle("second");
        PendingBoundParticles.send(session, 42, first);
        PendingBoundParticles.send(session, 42, second);
        PendingBoundParticles.send(session, 42, third);
        assertTrue(sent.isEmpty());
        PendingBoundParticles.flush(session, 42, 77);
        assertEquals(List.of(first, second, third), sent);
        assertEquals(Optional.of("first"), first.getMolangVariablesJson());
        assertEquals(Optional.of("second"), second.getMolangVariablesJson());
        sent.forEach(p -> { assertEquals(77L, p.getUniqueEntityId()); assertEquals(Vector3f.ZERO, p.getPosition()); });
        timers.forEach(t -> verify(t).cancel(false));
        PendingBoundParticles.flush(session, 42, 77);
        expiries.forEach(Runnable::run);
        assertEquals(3, sent.size());
    }

    @Test void readyCarrierDoesNotSuppressImmediateIdenticalPlayback() {
        Entity carrier = mock(Entity.class);
        when(carrier.isValid()).thenReturn(true);
        when(carrier.getGeyserId()).thenReturn(77L);
        when(cache.getEntityByJavaId(42)).thenReturn(carrier);
        PendingBoundParticles.send(session, 42, particle("same"));
        PendingBoundParticles.send(session, 42, particle("same"));
        assertEquals(2, sent.size());
        assertTrue(expiries.isEmpty());
    }

    @Test void expiryDropsOnlyItsOwnRequestAndNeverCreatesWorldEmitter() {
        PendingBoundParticles.send(session, 42, particle("old"));
        PendingBoundParticles.send(session, 42, particle("new"));
        expiries.getFirst().run();
        PendingBoundParticles.flush(session, 42, 77);
        assertEquals(1, sent.size());
        assertEquals(Optional.of("new"), sent.getFirst().getMolangVariablesJson());
    }

    @Test void lateExpiryCannotDeleteNewQueueForReusedEntityId() {
        PendingBoundParticles.send(session, 42, particle("old"));
        PendingBoundParticles.flush(session, 42, 77);
        PendingBoundParticles.send(session, 42, particle("new"));
        expiries.getFirst().run();
        PendingBoundParticles.flush(session, 42, 88);
        assertEquals(2, sent.size());
        assertEquals(88L, sent.getLast().getUniqueEntityId());
    }

    @Test void disconnectCancelsPendingAndDelayedCallbacksCannotReviveIt() {
        PendingBoundParticles.send(session, 42, particle("pending"));
        PendingBoundParticles.clear(session);
        timers.forEach(t -> verify(t).cancel(false));
        expiries.forEach(Runnable::run);
        PendingBoundParticles.flush(session, 42, 77);
        when(session.isClosed()).thenReturn(true);
        PendingBoundParticles.send(session, 42, particle("after-close"));
        assertTrue(sent.isEmpty());
    }
}
