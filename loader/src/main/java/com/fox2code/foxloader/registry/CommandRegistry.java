/*
 * MIT License
 * 
 * Copyright (c) 2023-2025 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.foxloader.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.command.ClientCommandCompletion;
import net.minecraft.common.command.Command;
import net.minecraft.common.command.IllegalCmdListenerOperation;
import net.minecraft.common.command.PlayerCommandHandler;

import java.util.*;

public final class CommandRegistry {
    private static final HashMap<String, Command> commandsMap = new HashMap<>();
    private static final HashMap<String, Command> clientCommandsMap = new HashMap<>();
    private static final HashMap<String, String> clientCommandsAliasMap = new HashMap<>();
    public static final Map<String, Command> commands = Collections.unmodifiableMap(commandsMap);
    public static final Map<String, Command> clientCommands = Collections.unmodifiableMap(clientCommandsMap);

    private CommandRegistry() { throw new AssertionError(); }

    public static void registerCommand(Command command) {
        if (Internal.registered) throw new IllegalStateException("Commands already registered");
        commandsMap.put(command.getName(), command);
    }

    public static void registerClientCommand(Command command) {
        if (Internal.registered) throw new IllegalStateException("Commands already registered");
        String commandName = command.getName();
        if (clientCommandsMap.put(commandName, command) != null) {
            clientCommandsAliasMap.values().remove(commandName);
        }
        clientCommandsAliasMap.remove(commandName);
        String[] aliases = command.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                if (!clientCommandsMap.containsKey(alias)) {
                    clientCommandsAliasMap.put(alias, commandName);
                }
            }
        }
    }

    public static final class Internal {
        private static boolean registered = false;

        public static void register() {
            if (registered || !Command.isCommandsLoaded) return;
            registered = true;
            PlayerCommandHandler.commands.addAll(commands.values());
        }

        public static boolean handleClientCommand(String command) {
            String[] arguments = PlayerCommandHandler.splitCommandIntoArgs(command, false);
            if (arguments.length == 0) {
                return false;
            }
            String commandName = arguments[0];
            if (commandName.startsWith("/")) {
                commandName = commandName.substring(1);
            }
            Command clientCommand = clientCommands.get(
                    clientCommandsAliasMap.getOrDefault(commandName, commandName));
            if (clientCommand != null) {
                try {
                    clientCommand.onExecute(arguments, Minecraft.getInstance().thePlayer);
                } catch (IllegalCmdListenerOperation e) {
                    Minecraft.getInstance().thePlayer.log(e.getMessage());
                }
                return true;
            }
            return false;
        }

        public static String handleClientCompletions(
                ClientCommandCompletion clientCommandCompletion,
                ArrayList<String> completionsResults, String[] arguments) {
            if (arguments.length == 1) {
                String ret =  clientCommandCompletion.complete(completionsResults, arguments);
                for (String clientCommand : clientCommands.keySet()) {
                    if (clientCommand.startsWith(arguments[0]) &&
                            !completionsResults.contains(clientCommand)) {
                        completionsResults.add(clientCommand);
                    }
                }
                return ret;
            }
            Command clientCommand = clientCommands.get(
                    clientCommandsAliasMap.getOrDefault(arguments[0], arguments[0]));
            if (clientCommand != null) {
                return clientCommand.getCommandCompletion().complete(
                        Minecraft.getInstance().thePlayer, completionsResults, arguments, 1);
            }
            return clientCommandCompletion.complete(completionsResults, arguments);
        }
    }
}
