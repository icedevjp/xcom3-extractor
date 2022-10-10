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
	
	public static File selectUserIsoFile() {
		File isoFile = new File("cd.iso");
		
		if(isoFile.exists()) {
			return isoFile;
		}
		
		FileDialog fd = new FileDialog((Frame) null, "Select ISO or BIN file");
		fd.setMultipleMode(false);
		fd.show(); // this will block until dialog is closed
		fd.dispose();
		
		String name = fd.getFile();
		if(name != null) {
			return new File(fd.getDirectory(), name);
		}
		
		return null;
	}
	

	public static void main(String[] args) throws Exception {
		File isoFile = selectUserIsoFile();
		
		IsoArchive iso = new IsoArchive(isoFile, isoFile.getName().toLowerCase().endsWith(".iso") == false); // provide path to xcom apocalypse iso
		PckExtractor pck = new PckExtractor();

		System.out.println("load palette");
		IsoEntry pal = iso.get("XCOM3/UFODATA/PAL_01.DAT");
		pck.decodePalette(pal.open(), (int) (pal.length/3));
		
		System.out.println("load city sprites");
		IsoEntry cityTab = iso.get("XCOM3/UFODATA/CITY.TAB");
		IsoEntry cityPck = iso.get("XCOM3/UFODATA/CITY.PCK");
		Sprite[] sprites = pck.decode(cityTab.open(), cityPck, (int) (cityTab.length/4));

		System.out.println("save png");
		File png = new File("CITY.PNG");
		saveSprites(Arrays.asList(sprites), png);
		
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
