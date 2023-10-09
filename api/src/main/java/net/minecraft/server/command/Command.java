package net.minecraft.server.command;

// https://git.derekunavailable.direct/Dereku/ReIndevPatches
public abstract class Command {

    public Command(final String name, final String[] aliases) {
    }

    public abstract void execute(String commandLabel, String[] args, CommandSender commandSender);

    public boolean onlyForOperators() {
        return true;
    }

    public boolean hideCommandArgs() {
        return false;
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    protected final void setIssuerRole(IssuerRole role) {
    }
}
