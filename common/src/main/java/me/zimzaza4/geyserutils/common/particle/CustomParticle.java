package me.zimzaza4.geyserutils.common.particle;

import com.fasterxml.jackson.annotation.JsonInclude;
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
     * <p>
     * ★ Wire compatibility (A1): when unbound (&lt; 0) this field is OMITTED from the serialized JSON,
     * so unbound particles (i.e. every existing particle) are byte-identical on the wire to the
     * pre-entityId format. This protects already-deployed old JE clients / Geyser extensions whose
     * CustomParticle has no entityId field and would otherwise fail on the unknown property. Only when
     * actually binding (entityId &gt;= 0) does the field appear — and that only happens for new content
     * whose clients are updated in lockstep.
     */
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = CustomParticle.UnboundEntityIdFilter.class)
    private int entityId = -1;

    /** Backward-compatible 3-arg constructor: world-space (unbound) particle. */
    public CustomParticle(Pos position, String identifier, @Nullable String molangVariablesJson) {
        this.position = position;
        this.identifier = identifier;
        this.molangVariablesJson = molangVariablesJson;
        this.entityId = -1;
    }

    /**
     * Jackson value filter: treats any negative entityId (the unbound sentinel, default -1) as "empty"
     * so it is omitted from serialization. equals() returning true means "skip this value".
     */
    public static final class UnboundEntityIdFilter {
        @Override
        public boolean equals(Object value) {
            return !(value instanceof Integer) || ((Integer) value) < 0;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
