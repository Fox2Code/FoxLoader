# ReIndevFoxLoader

ModLoader for Minecraft ReIndev

## Community

You can [join the official ReIndev Discord here](https://discord.gg/38Vfes6NpR)

A feature missing to make your mod? [Just open an issue!](https://github.com/Fox2Code/FoxLoader/issues)

## Installation

For client side installation, just run the jar file.
Either by double clicking on it, or running `java -jar FoxLoader.jar`

To run FoxLoader as a server just run `java -jar FoxLoader.jar --server`

## Documentation

For mixins usage check here: https://github.com/2xsaiko/mixin-cheatsheet

For spark usage check here: https://spark.lucko.me/docs/Command-Usage

For example mod check here: https://github.com/Fox2Code/FoxLoaderExampleMod

## Project structure
- `betacraft` -> BetaCraft launch method to run fox loader
- `client` -> client specific code
- `common` -> main component of the mod loader that are shared both on client and on server
- `dev` -> Gradle plugin
- `final` -> Installer a format in which the mod loader is shipped to user
- `server` -> server specific code

## Boot process

- Load pre patches
- Load core-mods (Aka. Legacy mods) into class loader
- Load pre-patches (FoxLoader asm patches)
- Load mods into class loader
- Initialize mixins
- Allow game to be loaded
- Load mods
- Launch game

As you see the game is allowed to be loaded very late into the boot process, even after mixins.

This ensures that all patches introduced by mods are applied, 
but also prevent code loaded in MixinPlugins to load Minecraft classes,
please keep that in mind when messing with mixins.

## Lua mods

If you are too lazy to learn java you can just use put `.lua` files in your `mods` folder

```lua
-- modId: lua_example
-- modName: Lua Example Mod
-- version: 1.0.0
-- description: Just add /discord to the game.
--

print("Hello from lua")

gameRegistry.registerCommand(function(args, player)
    player:displayChatMessage(chatColors.AQUA .. "https://discord.gg/38Vfes6NpR")
end, "discord", false) -- false is needed cause it default to "opOnly = true"

mod.onNetworkPlayerJoined = function(player)
    player:displayChatMessage(chatColors.AQUA .. "Welcome back to server name!")
end
```

Note: You can use the [`luajava`](https://github.com/luaj/luaj#user-content-luajava) extension from [LuaJ](https://github.com/luaj/luaj)
