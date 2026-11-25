package me.zimzaza4.geyserutils.common.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 服务端接管的一次挖掘；连接代际和单调编号隔离旧目标，终态只用于结束视觉预测。 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class ServerBreakStatePacket extends CustomPayloadPacket {
    public static final int VERSION = 1;
    private int version;
    private long generation;
    private long claim;
    private long revision;
    private int x;
    private int y;
    private int z;
    private float progress;
    private float progressPerTick;
    /** 0=start/update, 1=committed, 2=cancelled. */
    private int status;
}
