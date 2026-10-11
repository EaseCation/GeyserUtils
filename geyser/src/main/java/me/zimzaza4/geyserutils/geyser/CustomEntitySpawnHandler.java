package me.zimzaza4.geyserutils.geyser;

import com.google.common.cache.Cache;
import java.util.Map;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.event.type.JavaEntitySpawnEvent;

/** Applies only a previously announced, registered model definition to this spawn. */
final class CustomEntitySpawnHandler {
    private CustomEntitySpawnHandler() { }

    static void apply(JavaEntitySpawnEvent event, Cache<Integer, String> announced,
                      Map<String, ? extends EntityDefinition<?>> definitions) {
        if (announced == null) return;
        String identifier = announced.asMap().remove(event.packet().getEntityId());
        EntityDefinition<?> definition = identifier == null ? null : definitions.get(identifier);
        if (definition == null) return;

        var packet = event.packet();
        Entity original = event.originalEntity();
        event.entity(new Entity(event.session(), original.getEntityId(), original.getGeyserId(),
                original.getUuid(), definition, Vector3f.from(packet.getX(), packet.getY(), packet.getZ()),
                packet.getMovement().toFloat(), packet.getYaw(), packet.getPitch(), packet.getHeadYaw()));
    }
}
