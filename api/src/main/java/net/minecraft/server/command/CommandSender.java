package net.minecraft.server.command;


// https://git.derekunavailable.direct/Dereku/ReIndevPatches
public class CommandSender {
    public boolean isPlayer() {
        return false;
    }

    public void sendMessage(String message) {}

    /* public EntityPlayerMP getPlayer(); */
}
