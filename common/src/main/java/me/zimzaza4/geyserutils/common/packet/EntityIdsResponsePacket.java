package me.zimzaza4.geyserutils.common.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 两个数组逐项对应；missingIds 表示尚未跟踪，负数 Bedrock ID 仍是合法结果。 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EntityIdsResponsePacket extends CustomPayloadPacket {
    private long requestId;
    private int[] javaIds;
    private long[] bedrockIds;
    private int[] missingIds;
}
