package me.zimzaza4.geyserutils.geyser;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.event.type.JavaEntitySpawnEvent;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomEntitySpawnHandlerTest {
    @Test void replacesOnlyAnnouncedModelsAndPreservesSpawnIdentityAndPose() {
        GeyserSession session = mock(GeyserSession.class);
        Entity original = mock(Entity.class);
        UUID uuid = UUID.randomUUID();
        when(original.getEntityId()).thenReturn(42);
        when(original.getGeyserId()).thenReturn(77L);
        when(original.getUuid()).thenReturn(uuid);
        var packet = mock(ClientboundAddEntityPacket.class);
        when(packet.getEntityId()).thenReturn(42);
        when(packet.getX()).thenReturn(1d);
        when(packet.getY()).thenReturn(2d);
        when(packet.getZ()).thenReturn(3d);
        when(packet.getMovement()).thenReturn(Vector3d.from(4, 5, 6));
        when(packet.getYaw()).thenReturn(10f);
        when(packet.getPitch()).thenReturn(20f);
        when(packet.getHeadYaw()).thenReturn(30f);
        EntityDefinition<Entity> definition = new EntityDefinition<>(null, null, "test:model", .1f, .1f, 0,
                new GeyserEntityProperties(), List.of());
        Cache<Integer, String> announced = CacheBuilder.newBuilder().build();
        var event = new JavaEntitySpawnEvent(session, packet, original);
        CustomEntitySpawnHandler.apply(event, announced, Map.of("test:model", definition));
        assertSame(original, event.entity());
        announced.put(42, "missing");
        CustomEntitySpawnHandler.apply(event, announced, Map.of("test:model", definition));
        assertSame(original, event.entity());
        announced.put(42, "test:model");
        CustomEntitySpawnHandler.apply(event, announced, Map.of("test:model", definition));
        Entity replacement = event.entity();
        assertNotSame(original, replacement);
        assertSame(definition, replacement.getDefinition());
        assertEquals(42, replacement.getEntityId());
        assertEquals(77L, replacement.getGeyserId());
        assertEquals(uuid, replacement.getUuid());
        assertEquals(Vector3f.from(1, 2, 3), replacement.getPosition());
        assertEquals(Vector3f.from(4, 5, 6), replacement.getMotion());
        assertEquals(10f, replacement.getYaw());
        assertEquals(20f, replacement.getPitch());
        assertEquals(30f, replacement.getHeadYaw());
        assertNull(announced.getIfPresent(42));
        var reused = new JavaEntitySpawnEvent(session, packet, original);
        CustomEntitySpawnHandler.apply(reused, announced, Map.of("test:model", definition));
        assertSame(original, reused.entity());
    }
}
