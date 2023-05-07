package com.fox2code.foxloader.registry;

public interface RegisteredTileEntity {
    default RegisteredWorld getCurrentRegisteredWorld() { throw new RuntimeException(); }

    default int getRegisteredX() { throw new RuntimeException(); }

    default int getRegisteredY() { throw new RuntimeException(); }

    default int getRegisteredZ() { throw new RuntimeException(); }

    default RegisteredBlock getRegisteredBlock() { throw new RuntimeException(); }
}
