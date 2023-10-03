package net.minecraft.server.command;

// https://git.derekunavailable.direct/Dereku/ReIndevPatches
public abstract class Command {

    private final String commandLabel;
    private final String[] aliases;
    private IssuerRole canUseThisCommand = IssuerRole.BOTH;

    public Command(final String name) {
        this.commandLabel = name;
        this.aliases = new String[0];
    }

    public Command(final String name, final String[] aliases) {
        this.commandLabel = name;
        this.aliases = aliases;
    }

    public abstract void execute(String commandLabel, String[] args, CommandSender commandSender);

    public String getCommandLabel() {
        return commandLabel;
    }

    public String[] getAliases() {
        return aliases;
    }

    public boolean onlyForOperators() {
        return true;
    }

    public boolean hideCommandArgs() {
        return false;
    }

    public IssuerRole getRoleToUseThisCommand() {
        return canUseThisCommand;
    }

    protected final void setIssuerRole(IssuerRole role) {
        this.canUseThisCommand = role;
    }

    protected final int parseInt(String input, int value) {
        if (input.matches("-?[0-9]*")) {
            return Integer.parseInt(input);
        } else {
            return value;
        }
    }
}
