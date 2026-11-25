package me.zimzaza4.geyserutils.common.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 限定当前连接的有界诊断，不改变世界或挖掘进度。 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class ServerBreakTracePacket extends CustomPayloadPacket {
    private int seconds;
    private String traceId;
}
