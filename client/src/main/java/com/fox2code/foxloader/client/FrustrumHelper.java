package com.fox2code.foxloader.client;

import net.minecraft.src.client.physics.AxisAlignedBB;
import net.minecraft.src.client.physics.ClippingHelperImpl;
import net.minecraft.src.client.renderer.Frustrum;

/**
 * Hook {@link net.minecraft.src.client.renderer.EntityRenderer#renderWorld(float, long)}
 */
public class FrustrumHelper {
    public static final Frustrum frustrum = new Frustrum();

    public static boolean isBoundingBoxInFrustum(AxisAlignedBB box) {
        return frustrum.isBoundingBoxInFrustum(box);
    }

    public static boolean isBoundingBoxInFrustumFully(AxisAlignedBB box) {
        return frustrum.isBoundingBoxInFrustumFully(box);
    }

    public static class Hooks {
        public static void update(Frustrum frustrum, double x, double y, double z) {
            ClippingHelperImpl.getInstance();
            frustrum.setPosition(x, y, z);
        }
    }
}
