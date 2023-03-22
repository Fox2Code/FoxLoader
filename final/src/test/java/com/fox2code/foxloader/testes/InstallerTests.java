package com.fox2code.foxloader.testes;

import com.fox2code.foxloader.installer.Main;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InstallerTests {
    @Test
    public void testPojavDetector() {
        Assertions.assertTrue(Main.isPojavLauncherHome(
                "/storage/emulated/0/Android/data/net.kdt.pojavlaunch/files"));
        Assertions.assertFalse(Main.isPojavLauncherHome("/root"));
        Assertions.assertFalse(Main.isPojavLauncherHome("/home/pojavlaunch"));
    }
}
