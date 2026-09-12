package me.zimzaza4.geyserutils.spigot;

import lombok.Getter;
import me.zimzaza4.geyserutils.common.camera.data.CameraPreset;
import me.zimzaza4.geyserutils.common.channel.GeyserUtilsChannels;
import me.zimzaza4.geyserutils.common.manager.PacketManager;
import me.zimzaza4.geyserutils.common.packet.CustomPayloadPacket;
import me.zimzaza4.geyserutils.common.packet.NpcFormResponseCustomPayloadPacket;
import me.zimzaza4.geyserutils.common.packet.EntityIdsResponsePacket;
import me.zimzaza4.geyserutils.spigot.api.EntityIdQueries;
import me.zimzaza4.geyserutils.spigot.api.form.NpcDialogueForm;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

public final class GeyserUtils extends JavaPlugin {

    @Getter
    private static GeyserUtils instance;

    @Getter
    private static PacketManager packetManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        packetManager = new PacketManager();
        Messenger messenger = this.getServer().getMessenger();

        CameraPreset.load();
        messenger.registerOutgoingPluginChannel(this, GeyserUtilsChannels.MAIN);
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void quit(org.bukkit.event.player.PlayerQuitEvent event) {
                EntityIdQueries.cancelViewer(event.getPlayer().getUniqueId());
            }
            @org.bukkit.event.EventHandler
            public void world(org.bukkit.event.player.PlayerChangedWorldEvent event) {
                EntityIdQueries.cancelViewer(event.getPlayer().getUniqueId());
            }
        }, this);
        messenger.registerIncomingPluginChannel(this, GeyserUtilsChannels.MAIN, (channel, player, message) -> {
            if (channel.equals(GeyserUtilsChannels.MAIN)) {
                CustomPayloadPacket packet = packetManager.decodePacket(message);
                if (packet instanceof EntityIdsResponsePacket response) {
                    EntityIdQueries.accept(player, response);
                    return;
                }
                if (packet instanceof NpcFormResponseCustomPayloadPacket) {
                    NpcFormResponseCustomPayloadPacket response = (NpcFormResponseCustomPayloadPacket) packet;
                    if (NpcDialogueForm.FORMS.containsKey(response.getFormId())) {

                        NpcDialogueForm form = NpcDialogueForm.FORMS.get(response.getFormId());

                        if (form.handler() != null) {
                            if (response.getButtonId() != -1) {
                                form.handler().accept(response.getFormId(), response.getButtonId());
                            }
                        }
                        if (response.getButtonId() == -1) {
                            if (form.closeHandler() != null) {
                                form.closeHandler().accept(response.getFormId());
                            }
                            NpcDialogueForm.FORMS.remove(response.getFormId());
                        }


                    }
                }
            }
        });

        // Test command: send a Bedrock particle (geyserutils:main) to test the geyserutilsbridge client mod
        org.bukkit.command.PluginCommand bparticle = getCommand("bparticle");
        if (bparticle != null) {
            bparticle.setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage("Only players can run this command.");
                    return true;
                }
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                if (args.length < 1) {
                    player.sendMessage("Usage: /bparticle <identifier> [molangJson]");
                    return true;
                }
                String identifier = args[0];
                String molang = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : null;
                try {
                    // CustomParticle(position, identifier, molangVariablesJson) — position 由 sendCustomParticle 的 location 决定，传 null
                    me.zimzaza4.geyserutils.common.particle.CustomParticle particle =
                            new me.zimzaza4.geyserutils.common.particle.CustomParticle(null, identifier, molang);
                    me.zimzaza4.geyserutils.spigot.api.PlayerUtils.sendCustomParticle(player, player.getLocation(), particle);
                    player.sendMessage("Sent Bedrock particle: " + identifier + " (geyserutils:main -> geyserutilsbridge client mod)");
                } catch (Throwable t) {
                    player.sendMessage("Failed to send particle: " + t.getMessage());
                }
                return true;
            });
        }
    }

    @Override
    public void onDisable() {
        EntityIdQueries.shutdown();
    }

}
