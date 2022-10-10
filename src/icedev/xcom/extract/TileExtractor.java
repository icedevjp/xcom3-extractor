package icedev.xcom.extract;

import java.io.*;

import icedev.io.LittleEndianInputStream;
import icedev.io.IsoArchive.IsoEntry;

public class TileExtractor {
	/** Tile DAT file structure as reversed by guys at OpenApoc */
	public static class TileEntry {
		public static final int TILE_TYPE_GENERAL = (0x00);
		public static final int TILE_TYPE_ROAD = (0x01);
		public static final int TILE_TYPE_PEOPLE_TUBE_JUNCTION = (0x02);
		public static final int TILE_TYPE_PEOPLE_TUBE = (0x03);
		public static final int TILE_TYPE_CITY_WALL = (0x04);

		public static final int ROAD_TYPE_STRAIGHT_BEND = (0x00);
		public static final int ROAD_TYPE_JUNCTION = (0x01);
		public static final int ROAD_TYPE_TERMINAL = (0x02);

		public static final int WALK_TYPE_NONE = (0x00);
		public static final int WALK_TYPE_INTO = (0x01);
		public static final int WALK_TYPE_ONTO = (0x02);
		
		public int tile_type;            // TILE_TYPE_*
		public int road_type;            // ROAD_TYPE_*
		public final int[] road_junction = new int[4];     // 0 = not connect, 1 = is connected, in N E S W order
		public final int[] road_level_change = new int[4]; // 0 = flat, 1 = up, 0xFF (-1) = down, in NESW order
		public int height;               // 0-16
		public int constitution;         // 0-255
		public int value;                // 0-255
		public int overlaytile_idx; // Offset of an 'overlay' sprite in ufodata/cityovr.pck - 0xff appears
		                         // to mean "none"
		public int landing_pad;     // 1 == landing pad
		public int walk_type;       // WALK_TYPE_*
		public final int[] voxelIdx = new int[16];
		public int has_basement;               // 0 = no basement, 1 = has basement
		public final int[] people_tube_connections = new int[6]; // 0=not connected, 1 = connected, in N E S W Up Down order
		public int mass;                       // 0-10
		public int strength;                   // 0-100
		public int unknown3;
		public int damagedtile_idx; //uint16
		public int supportile_idx; //uint16
		public final int[] unknown4 = new int[4];
		public int stratmap_idx; //uint16
		

		public String toString() {
			return "TileEntry[tile_type="+tile_type+", road_type="+road_type+", height="+height+"]";
		}
	}
	
	public TileEntry readTileEntry(LittleEndianInputStream in) throws IOException {
		TileEntry dto = new TileEntry();
		
		dto.tile_type = in.readUint8();
		dto.road_type = in.readUint8();
		for(int i=0; i<dto.road_junction.length; i++) {
			dto.road_junction[i] = in.readUint8();
		}
		for(int i=0; i<dto.road_level_change.length; i++) {
			dto.road_level_change[i] = in.readUint8();
		}
		dto.height = in.readUint8();
		dto.constitution = in.readUint8();
		dto.value = in.readUint8();
		dto.overlaytile_idx = in.readUint8();
		dto.landing_pad = in.readUint8();
		dto.walk_type = in.readUint8();
		for(int i=0; i<dto.voxelIdx.length; i++) {
			dto.voxelIdx[i] = in.readUint8();
		}
		dto.has_basement = in.readUint8();
		for(int i=0; i<dto.people_tube_connections.length; i++) {
			dto.people_tube_connections[i] = in.readUint8();
		}
		dto.mass = in.readUint8();
		dto.strength = in.readUint8();
		dto.unknown3 = in.readUint8();
		dto.damagedtile_idx = in.readUint16();
		dto.supportile_idx = in.readUint16();
		for(int i=0; i<dto.unknown4.length; i++) {
			dto.unknown4[i] = in.readUint8();
		}
		dto.stratmap_idx = in.readUint16();
		
		return dto;
	}

	public TileEntry[] readEntries(LittleEndianInputStream in, int fileSize) throws IOException {
		if(fileSize % 52 != 0)
			throw new IOException("Tile DAT file length must be a multiple of 52");
		
		TileEntry[] entries = new TileEntry[(int) (fileSize / 52)];
		for(int i=0; i<entries.length; i++) {
			entries[i] = readTileEntry(in);
		}
		return entries;
	}
	
	public TileEntry[] load(File tdat) throws IOException {
		try(var in = LittleEndianInputStream.wrap(tdat)) {
			return readEntries(in, (int) tdat.length());
		}
	}
	
	public TileEntry[] load(IsoEntry tdat) throws IOException {
		return readEntries(tdat.open(), (int) tdat.length);
	}
}
