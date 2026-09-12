package me.zimzaza4.geyserutils.common.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 查询当前观察者实际收到的实体 ID；不创建实体，也不表示渲染成功。 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EntityIdsRequestPacket extends CustomPayloadPacket {
    public static final int MAX_ENTITIES = 64;
    private long requestId;
    private int[] javaIds;
}
