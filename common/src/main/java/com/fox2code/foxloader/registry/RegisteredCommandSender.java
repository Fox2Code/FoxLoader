package com.fox2code.foxloader.registry;

import com.fox2code.foxloader.loader.ModLoader;

public interface RegisteredCommandSender {
    RegisteredCommandSender CONSOLE_COMMAND_SENDER = new RegisteredCommandSender() {
        @Override
        public void displayChatMessage(String chatMessage) {
            if (chatMessage.indexOf('\n') == -1) {
                ModLoader.getModLoaderLogger().info(chatMessage);
            } else {
                String[] splits = chatMessage.split("\\n");
                for (String split : splits) {
                    ModLoader.getModLoaderLogger().info(split);
                }
            }
        }

        @Override
        public boolean isOperator() {
            return true;
        }
    };

    /**
     * Send/Display chat message to the user screen or console.
     */
    default void displayChatMessage(String chatMessage) { throw new RuntimeException(); }

    /**
     * @return if the command sender as operator permission.
     *
     * Always false client-side when connected to a server.
     */
    default boolean isOperator() { throw new RuntimeException(); }
}
