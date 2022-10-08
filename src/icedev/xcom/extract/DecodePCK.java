package icedev.xcom.extract;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import icedev.io.*;

/**
 * https://www.ufopaedia.org/index.php/Image_Formats_(Apocalypse)#PCK
 */
public class DecodePCK {
	private static final int[] shadowPalette = {0, 0x80000000, 0x80000000, 0x40000000, 0x40000000, 0x40000000, 0x40000000, 0x40000000, 0x40000000};
	
	public static interface DataSupplier {
		public LittleEndianInputStream seek(int offset) throws IOException;
	}
	
	public static interface ImageSupplier {
		public BufferedImage create(int width, int height);
	}
	
	private static class RafDataSupplier implements DataSupplier {
		RandomAccessFile raf;
		FileInputStream fis;
		
		public RafDataSupplier(RandomAccessFile raf) throws IOException {
			this.raf = raf;
			this.fis = new FileInputStream(raf.getFD());
		}

		@Override
		public LittleEndianInputStream seek(int offset) throws IOException {
			raf.seek(offset);
			return LittleEndianInputStream.wrap(fis);
		}
		
	}

	public static int image_type = BufferedImage.TYPE_INT_ARGB_PRE;
	public ImageSupplier images = (w,h) -> new BufferedImage(w, h, image_type);
	public Color[] palette;
	
	public void decodePalette(LittleEndianInputStream in, int num) throws IOException {
		//this.a = new int[n];
		this.palette = new Color[num];
		
		for(int n=0; n<num; n++) {
			int red = in.readUint8();
			int gre = in.readUint8();
			int blu = in.readUint8();
			
			int a = 255;
			int r = (red << 2 | (red >> 4 & 0x3)) & 0xFF;
			int g = (gre << 2 | (gre >> 4 & 0x3)) & 0xFF;
			int b = (blu << 2 | (blu >> 4 & 0x3)) & 0xFF;
			if (n == 0) {
				r=g=b=a=0;
			}
			this.palette[n] = new Color(r, g, b, a);
		}
	}
	

	public void decodeShadow(LittleEndianInputStream tab, DataSupplier pck, List<Sprite> out) throws IOException {
		while(tab.available() > 0) {
			int fileOffset = tab.readInt32();
			LittleEndianInputStream in = pck.seek(fileOffset);
			
			in.readUint8(); // compression type
			in.readUint8(); // ???
			in.readUint16(); // ???
			char width = in.readUint16();
			char height = in.readUint16();
			BufferedImage image = images.create(width, height);
			int[] pixels = new int[width * height];
			int offset = 0;
			int count = 0;
			while ((count = in.readUint8()) != 0xFF) {
				int shadowIndex = in.readUint8();
				if (shadowIndex == 0) {
					offset += count << 2;
				} else {
					while (count-- > 0) {
						for (int j = 0; j < 4; ++j) {
							int x = (offset % 640);
							int y = offset / 640 ;
							if (x < width && y < height) {
								pixels[y * width + x] =
										shadowPalette[shadowIndex];
							}
							offset+=1;
						}
					}
				}
			}
			image.setRGB(0, 0, width, height, pixels, 0, width);
			out.add(new Sprite(image, 0, 0));
		}
	}
	
	public void decode(LittleEndianInputStream tab, DataSupplier pck, List<Sprite> out) throws IOException {
		while(tab.available() > 0) {
			int fileOffset = tab.readInt32();
			LittleEndianInputStream in =pck.seek(fileOffset << 2);
			
			int compressionType = in.readUint8();
			
			int unused1 = in.readUint8(); // ignore
			int unused2 = in.readUint8(); // ignore
			int unused3 = in.readUint8(); // ignore
			
			int left   = in.readUint16();
			int right  = in.readUint16();
			int top    = in.readUint16();
			int bottom = in.readUint16();
			
			//System.out.println("metrics: " + left + ", " + right + ", " + top + ", " + bottom + " at offset " + fileOffset);
			
			char width = (char) (right - left);
			char height = (char) (bottom - top);
			
			BufferedImage image = images.create(width, height);
			int[] pixels = new int[width * height];
			if (compressionType == 1) {
				int coordinate;
				while ((coordinate = in.readInt32()) != -1) {
					int targetY = coordinate / 640;
					
					int targetX = in.readUint8();
					int rowLen = in.readUint8();
					in.readUint8(); // left padding, ignore
					in.readUint8(); // right padding, ignore
					
					// Run Length Encoding
					for (int i = 0; i < rowLen; ++i) {
						int colorIndex = in.readUint8();
						int x = targetX + i - left;
						int y = targetY - top;
						if (x < width && y < height) {
							pixels[y * width + x] = palette[colorIndex].getRGB();
						}
					}
				}
				image.setRGB(0, 0, width, height, pixels, 0, width);
				//setPixels(img, array);
			} else if (compressionType == 2) {
				System.err.println("NOT IMPLEMENTED COMPRESSION TYPE 2");
			} else {
				System.err.println("Unknown compression: " + compressionType);
			}
			Sprite sprite = new Sprite(image, left, top);
			out.add(sprite);
		}
	}
	
	public void loadPalette(File palFile) throws IOException {
		try (var stream = LittleEndianInputStream.wrap(palFile)) {
			decodePalette(stream, (int) (palFile.length() / 3L));
		}
	}
	public List<Sprite> loadSprites(File pckFile, File tabFile) throws IOException {
		List<Sprite> list = new ArrayList<>();
		loadSprites(pckFile, tabFile, list);
		return list;
	}
	
	public void loadSprites(File pckFile, File tabFile, List<Sprite> out) throws IOException {
		try ( var tab = LittleEndianInputStream.wrap(tabFile);
		      RandomAccessFile pck = new RandomAccessFile(pckFile, "r");
				) {
			decode(tab, new RafDataSupplier(pck), out);
		}
	}
	
	public void loadShadows(File pckFile, File tabFile, List<Sprite> out) throws IOException {
		try ( var tab = LittleEndianInputStream.wrap(tabFile);
		      RandomAccessFile pck = new RandomAccessFile(pckFile, "r");
				) {
			decodeShadow(tab, new RafDataSupplier(pck), out);
		}
	}
}
