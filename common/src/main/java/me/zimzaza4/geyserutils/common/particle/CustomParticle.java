package me.zimzaza4.geyserutils.common.particle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.zimzaza4.geyserutils.common.util.Pos;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
@NoArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
public class CustomParticle {
    private Pos position;
    private String identifier;
    @Nullable
    private String molangVariablesJson;
    /**
     * Optional Java-side entity id to bind this particle's emitter to (follows the entity as it moves).
     * -1 (default) = unbound world-space emitter. Downstream (Geyser extension / JE bridge) maps this
     * Java entity id to the per-session bedrock geyserId / uses it directly on the client world.
     */
    private int entityId = -1;

    /** Backward-compatible 3-arg constructor: world-space (unbound) particle. */
    public CustomParticle(Pos position, String identifier, @Nullable String molangVariablesJson) {
        this.position = position;
        this.identifier = identifier;
        this.molangVariablesJson = molangVariablesJson;
        this.entityId = -1;
    }
}
