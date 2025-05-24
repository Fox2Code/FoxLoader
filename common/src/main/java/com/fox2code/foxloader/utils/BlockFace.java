package com.fox2code.foxloader.utils;

public enum BlockFace {
	DOWN (0, -1, 0),
	UP   (0, +1, 0),
	SOUTH(0, 0, -1),
	NORTH(0, 0, +1),
	WEST (-1, 0, 0),
	EAST (+1, 0, 0);

	public final int directionX, directionY, directionZ;

	BlockFace(int directionX, int directionY, int directionZ) {
		this.directionX = directionX;
		this.directionY = directionY;
		this.directionZ = directionZ;
	}
}
