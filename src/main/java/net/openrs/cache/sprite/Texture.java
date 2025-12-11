package net.openrs.cache.sprite;

import java.nio.ByteBuffer;

public class Texture {

	private int fileId;       // for 233+ single fileId
	private int[] fileIds;    // legacy multiple fileIds
	private int[] field2301;  // legacy
	private int[] field2296;  // legacy
	private int[] field2295;  // legacy

	private int averageRGB;
	private boolean isLowDetail; // field2293 in legacy
	private int animationDirection;
	private int animationSpeed;
	public static int  version = 233;

	/**
	 * Decode a texture from buffer based on version.
	 */
	public static Texture decode(ByteBuffer buffer) {
		Texture texture = new Texture();

		if (version >= 233) {
			// Version 233+ format
			texture.fileId = buffer.getShort() & 0xFFFF;
			texture.averageRGB = buffer.getShort() & 0xFFFF;
			texture.isLowDetail = (buffer.get() & 0xFF) == 1;
			texture.animationDirection = buffer.get() & 0xFF;
			texture.animationSpeed = buffer.get() & 0xFF;

			// map fileId to array for consistency
			texture.fileIds = new int[]{texture.fileId};

		} else {
			// Legacy format ≤232
			texture.averageRGB = buffer.getShort() & 0xFFFF;
			texture.isLowDetail = (buffer.get() & 0xFF) == 1; // field2293

			int count = buffer.get() & 0xFF;
			if (count < 1 || count > 4) {
				System.out.println("Texture out of range 1..4 [" + count + "]");
				count = Math.max(1, Math.min(count, 4));
			}

			texture.fileIds = new int[count];
			for (int i = 0; i < count; i++) {
				texture.fileIds[i] = buffer.getShort() & 0xFFFF;
			}

			if (count > 1) {
				texture.field2301 = new int[count - 1];
				for (int i = 0; i < count - 1; i++) {
					texture.field2301[i] = buffer.get() & 0xFF;
				}

				texture.field2296 = new int[count - 1];
				for (int i = 0; i < count - 1; i++) {
					texture.field2296[i] = buffer.get() & 0xFF;
				}
			}

			texture.field2295 = new int[count];
			for (int i = 0; i < count; i++) {
				texture.field2295[i] = buffer.getInt();
			}

			texture.animationDirection = buffer.get() & 0xFF;
			texture.animationSpeed = buffer.get() & 0xFF;
		}

		return texture;
	}

	// Getter: always returns single fileId (233+) or first legacy fileId
	public int getIds(int i) {
		return fileId;
	}

	// Optional: access legacy array
	public int getFileIds(int i) {
		if (fileIds != null) {
			return fileIds[i];
		}
		return fileId;
	}

	// Additional getters
	public int getAverageRGB() {
		return averageRGB;
	}

	public boolean isLowDetail() {
		return isLowDetail;
	}

	public int getAnimationDirection() {
		return animationDirection;
	}

	public int getAnimationSpeed() {
		return animationSpeed;
	}

	public int[] getField2301() {
		return field2301;
	}

	public int[] getField2296() {
		return field2296;
	}

	public int[] getField2295() {
		return field2295;
	}
}
