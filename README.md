# ReIndevFoxLoader

ModLoader for Minecraft ReIndev

## Documentation

For mixins usage check here: https://github.com/2xsaiko/mixin-cheatsheet

For spark usage check here: https://spark.lucko.me/docs/Command-Usage

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
