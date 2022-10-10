package icedev.xcom.extract;

import java.io.*;

import icedev.io.LittleEndianInputStream;
import icedev.io.IsoArchive.IsoEntry;


public class BldExtractor {
	/** BLD file structure as reversed by guys at OpenApoc */
	public static class BldEntry {
		public int name_idx; // This index is matched against building name table
		public int x0;
		public int x1;
		public int y0;
		public int y1;
		// 0x0A
		public final int[] unknown1 = new int[80]; // People tube data populated in savegame
		// 0xAA
		public int function_idx; // Specifies building function (aka type)
		public final int[] unknown2 = new int[5];
		// 0xB6
		public int is_purchaseable;
		public int unknown3_flag;
		public int unknown4;
		// 0xBA
		public int price; // Savegame value, divided by 2000
		public int alien_detection_unknown;
		public int maintenance_costs;
		public int maximum_workforce;
		public int current_workforce;
		public int current_wage;
		public int income_per_capita;
		// 0xC8
		public int owner_idx;
		public int unknown5;
		public int investment_value;
		public int respect_value;
		public final int[] unknown6 = new int[3];
		// 0xD4
		public final int[] alien_count = new int[14];
		
		public String toString() {
			return "BldEntry[name_idx="+name_idx+", function_idx="+function_idx+"]";
		}
	}

	public BldEntry readBldEntry(LittleEndianInputStream in) throws IOException {
		BldEntry dto = new BldEntry();
		
		dto.name_idx = in.readUint16();
		dto.x0 = in.readUint16();
		dto.x1 = in.readUint16();
		dto.y0 = in.readUint16();
		dto.y1 = in.readUint16();
		for(int i=0; i<dto.unknown1.length; i++) {
			dto.unknown1[i] = in.readUint16();
		}
		dto.function_idx = in.readUint16();
		for(int i=0; i<dto.unknown2.length; i++) {
			dto.unknown2[i] = in.readUint16();
		}
		dto.is_purchaseable = in.readUint8();
		dto.unknown3_flag = in.readUint8();
		dto.unknown4 = in.readUint16();
		dto.price = in.readUint16();
		dto.alien_detection_unknown = in.readUint16();
		dto.maintenance_costs = in.readUint16();
		dto.maximum_workforce = in.readUint16();
		dto.current_workforce = in.readUint16();
		dto.current_wage = in.readUint16();
		dto.income_per_capita = in.readUint16();
		dto.owner_idx = in.readUint16();
		dto.unknown5 = in.readUint16();
		dto.investment_value = in.readUint8();
		dto.respect_value = in.readUint8();
		for(int i=0; i<dto.unknown6.length; i++) {
			dto.unknown6[i] = in.readUint16();
		}
		for(int i=0; i<dto.alien_count.length; i++) {
			dto.alien_count[i] = in.readUint8();
		}
		
		return dto;
	}

	public BldEntry[] readEntries(LittleEndianInputStream in, int fileSize) throws IOException {
		if(fileSize % 226 != 0)
			throw new IOException("BLD file length must be a multiple of 226");
		
		BldEntry[] entries = new BldEntry[(int) (fileSize / 226)];
		for(int i=0; i<entries.length; i++) {
			entries[i] = readBldEntry(in);
		}
		return entries;
	}
	
	public BldEntry[] load(File bld) throws IOException {
		try(var in = LittleEndianInputStream.wrap(bld)) {
			return readEntries(in, (int) bld.length());
		}
	}
	
	public BldEntry[] load(IsoEntry bld) throws IOException {
		return readEntries(bld.open(), (int) bld.length);
	}
}
