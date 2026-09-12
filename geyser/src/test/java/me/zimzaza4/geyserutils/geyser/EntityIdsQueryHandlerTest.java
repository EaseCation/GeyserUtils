package me.zimzaza4.geyserutils.geyser;

import me.zimzaza4.geyserutils.common.packet.EntityIdsRequestPacket;
import me.zimzaza4.geyserutils.common.packet.EntityIdsResponsePacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.EntityCache;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntityIdsQueryHandlerTest {
    @Test void readsActualSignedMappingOnConnectionThread() {
        GeyserSession session=mock(GeyserSession.class);
        EntityCache cache=mock(EntityCache.class);
        Entity self=mock(Entity.class);
        when(session.getEntityCache()).thenReturn(cache);
        when(cache.getEntityByJavaId(42)).thenReturn(self);
        when(self.isValid()).thenReturn(true);
        when(self.getGeyserId()).thenReturn(-10L);
        List<Runnable> queue=new ArrayList<>();
        doAnswer(call->{queue.add(call.getArgument(0));return null;}).when(session).ensureInEventLoop(any());
        List<EntityIdsResponsePacket> responses=new ArrayList<>();
        EntityIdsQueryHandler.handle(session,new EntityIdsRequestPacket(7,new int[]{42,90}),responses::add);
        verifyNoInteractions(cache);
        assertTrue(responses.isEmpty());
        queue.getFirst().run();
        assertEquals(7,responses.getFirst().getRequestId());
        assertArrayEquals(new int[]{42},responses.getFirst().getJavaIds());
        assertArrayEquals(new long[]{-10},responses.getFirst().getBedrockIds());
        assertArrayEquals(new int[]{90},responses.getFirst().getMissingIds());
    }

    @Test void invalidAndOversizedRequestsDoNotReadSession() {
        GeyserSession session=mock(GeyserSession.class);
        EntityIdsQueryHandler.handle(session,new EntityIdsRequestPacket(1,null),r->fail());
        EntityIdsQueryHandler.handle(session,new EntityIdsRequestPacket(2,new int[65]),r->fail());
        verifyNoInteractions(session);
    }

    @Test void removedEntitiesAreMissingAndRequestArrayIsSnapshotted() {
        GeyserSession session=mock(GeyserSession.class);
        EntityCache cache=mock(EntityCache.class);
        Entity gone=mock(Entity.class);
        when(session.getEntityCache()).thenReturn(cache);
        when(cache.getEntityByJavaId(4)).thenReturn(gone);
        when(gone.isValid()).thenReturn(false);
        List<Runnable> queue=new ArrayList<>();
        doAnswer(call->{queue.add(call.getArgument(0));return null;}).when(session).ensureInEventLoop(any());
        int[] ids={4,4};List<EntityIdsResponsePacket> responses=new ArrayList<>();
        EntityIdsQueryHandler.handle(session,new EntityIdsRequestPacket(3,ids),responses::add);
        ids[0]=999;queue.getFirst().run();
        assertArrayEquals(new int[]{4},responses.getFirst().getMissingIds());
        verify(cache,times(1)).getEntityByJavaId(4);
        verify(cache,never()).getEntityByJavaId(999);
    }
}
