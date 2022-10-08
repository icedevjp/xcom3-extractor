package icedev.xcom.extract;

import java.awt.*;

public class Sprite {
	public Image img;
	public int offX;
	public int offY;
	public float orderCorrection;
	
//	public int left, right, top, bottom;

	public Sprite(Image img, int offX, int offY) {
		this.img = img;
		this.offX = offX;
		this.offY = offY;
	}

	public void draw(Graphics2D g, int x, int y) {
		g.drawImage(img, x + offX, y + offY, null);
	}

	@Override
	public String toString() {
		return "PCKImage [offX=" + offX + ", offY=" + offY + "]";
	}

	public void offset(int x, int y) {
		offX -= x;
		offY -= y;
	}
	
}
