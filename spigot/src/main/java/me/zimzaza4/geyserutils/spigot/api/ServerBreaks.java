package me.zimzaza4.geyserutils.spigot.api;

import me.zimzaza4.geyserutils.common.channel.GeyserUtilsChannels;
import me.zimzaza4.geyserutils.common.packet.ServerBreakStatePacket;
import me.zimzaza4.geyserutils.common.packet.ServerBreakTracePacket;
import me.zimzaza4.geyserutils.spigot.GeyserUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** 单连接能力门控；旧扩展不收到未知消息，所有调用在 Paper 主线程执行。 */
public final class ServerBreaks {
    private ServerBreaks() { }
    public static boolean supported(Player player) {
        return player.getListeningPluginChannels().contains(GeyserUtilsChannels.SERVER_BREAK_CAPABILITY);
    }
    public static boolean publish(Player player, long generation, long claim, long revision, int x, int y, int z,
                                  float progress, float progressPerTick, int status) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Server break update must run on the main thread");
        if (!supported(player)) return false;
        var packet = new ServerBreakStatePacket(1, generation, claim, revision, x, y, z, progress, progressPerTick, status);
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN, GeyserUtils.getPacketManager().encodePacket(packet));
        return true;
    }
    public static void trace(Player player, int seconds, String traceId) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Server break trace must run on the main thread");
        if (!supported(player)) return;
        player.sendPluginMessage(GeyserUtils.getInstance(), GeyserUtilsChannels.MAIN,
                GeyserUtils.getPacketManager().encodePacket(new ServerBreakTracePacket(seconds, traceId)));
    }
}
