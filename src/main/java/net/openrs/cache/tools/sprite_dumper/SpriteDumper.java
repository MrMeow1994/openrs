/**
* Copyright (c) Kyle Fricilone
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
package net.openrs.cache.tools.sprite_dumper;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import net.openrs.cache.Cache;
import net.openrs.cache.Constants;
import net.openrs.cache.Container;
import net.openrs.cache.FileStore;
import net.openrs.cache.ReferenceTable;
import net.openrs.cache.sprite.Sprite;
import net.openrs.util.ImageUtils;

/**
 * @author Kyle Friz
 * @since Dec 30, 2015
 */
public class SpriteDumper {
	
	public static void main(String[] args) throws IOException {
		File directory = new File(Constants.SPRITE_PATH_six_six_one);
		
		if (!directory.exists()) {
			directory.mkdirs();
		}
		
		try (Cache cache = new Cache(FileStore.open(Constants._CACHE_PATH_six_six_one))) {
			ReferenceTable table = cache.getReferenceTable(8);
			for (int i = 0; i < table.capacity(); i++) {
				if (table.getEntry(i) == null)
					continue;

				Container container = cache.read(8, i);
				Sprite sprite = Sprite.decode(container.getData());

				File transparentDir = new File(Constants.SPRITE_PATH_six_six_one, "transparent");
				File magentaDir = new File(Constants.SPRITE_PATH_six_six_one, "magenta");

// Create directories if missing
				transparentDir.mkdirs();
				magentaDir.mkdirs();

				for (int frame = 0; frame < sprite.size(); frame++) {
					BufferedImage frameImage = sprite.getFrame(frame);

					// Step 1: remove old magenta & white backgrounds (make them transparent)
					BufferedImage transparent = ImageUtils.makeMultiColorTransparent(
							frameImage,
							new Color[]{ Color.WHITE, new Color(255, 0, 255) },
							10 // tolerance — loosen if edges still show
					);

					// Step 2a: save as TRUE TRANSPARENT version
					File transparentFile = new File(transparentDir, i + "_" + frame + ".png");
					ImageIO.write(transparent, "png", transparentFile);

					// Step 2b: also create a MAGENTA background version (for legacy debugging)
					BufferedImage magentaBackground = ImageUtils.createColoredBackground(
							transparent,
							new Color(0xFF00FF) // magenta fill behind transparency
					);

					File magentaFile = new File(magentaDir, i + "_" + frame + ".png");
					ImageIO.write(magentaBackground, "png", magentaFile);
				}

				double progress = (double) (i + 1) / table.capacity() * 100;
				
				System.out.printf("%d out of %d %.2f%s\n", (i + 1), table.capacity(), progress, "%");	
				
			}

			Container container = cache.read(10, cache.getFileId(10, "title.jpg"));
			byte[] bytes = new byte[container.getData().remaining()];
			container.getData().get(bytes);
			Files.write(Paths.get(Constants.SPRITE_PATH).resolve("title.jpg"), bytes);

		}
	}

}
