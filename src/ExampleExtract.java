import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.imageio.ImageIO;

import icedev.io.*;
import icedev.io.IsoArchive.IsoEntry;
import icedev.xcom.extract.*;

public class ExampleExtract {

	public static void main(String[] args) throws Exception {
		IsoArchive iso = new IsoArchive(new File("cd.iso")); // provide path to xcom apocalypse iso
		DecodePCK pck = new DecodePCK();
		
		System.out.println("load palette");
		IsoEntry palFile = iso.get("/XCOM3/UFODATA/PAL_01.DAT");
		pck.decodePalette(LittleEndianInputStream.wrap(iso.open(palFile)), (int) (palFile.length / 3L));

		System.out.println("load city sprites");
		List<Sprite> sprites = new ArrayList<>();
		IsoEntry cityTab = iso.get("/XCOM3/UFODATA/CITY.TAB");
		IsoEntry cityPck = iso.get("/XCOM3/UFODATA/CITY.PCK");
		pck.decode(LittleEndianInputStream.wrap(iso.open(cityTab)), (offset)-> LittleEndianInputStream.wrap(iso.open(cityPck, offset)), sprites);

		System.out.println("save png");
		File png = new File("CITY.PNG");
		saveSprites(sprites, png);
		
		if(Desktop.isDesktopSupported()) {
			Desktop.getDesktop().open(png);
		}
	}
	
	
	public static void saveSprites(List<Sprite> sprites, File png) throws IOException {
		int tileSize = 64;
		int rowLen = 32;
		int rows = (int) Math.ceil( sprites.size() / (double) rowLen);
		int width = tileSize * rowLen;
		int height = tileSize * rows;
		
		BufferedImage canva = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = canva.createGraphics();
		
		int index = 0;
		board: for(int y=0; y<rows; y++) {
			for(int x=0; x<rowLen; x++) {
				Sprite sprite = sprites.get(index);
				g2d.drawImage(sprite.img, x*tileSize, y*tileSize, null);
				index++;
				
				if(sprites.size() <= index)
					break board;
			}
		}

		ImageIO.write(canva, "PNG", png);
	}

}
