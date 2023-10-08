package net.minecraft.server.command;


// https://git.derekunavailable.direct/Dereku/ReIndevPatches
public class CommandSender {
    public boolean isPlayer() {
        return false;
    }

    public void sendMessage(String message) {}

    // It's fine, I swear.
    public Object getPlayer() {
        return null;
    }
}
