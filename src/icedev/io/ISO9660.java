package icedev.io;

import java.io.*;
import java.util.*;

/** very minimalistic bare bones ISO9660 extractor https://wiki.osdev.org/ISO_9660 */
public class ISO9660 {
	RandomAccessFile raf;
	LittleEndianInputStream in;
	
	public ISO9660(RandomAccessFile raf) throws IOException {
		this.raf = raf;
		this.in = LittleEndianInputStream.wrap(raf);
	}
	
	public int blockSize;
	public int pathTableSize;
	public int pathTableLocation;
	
	private String systemIdentifier;
	private String volumeIdentifier;
	
	public void readVolumeDescriptor() throws IOException {
		raf.seek(1024 * 32);
		
		int volumeDescriptorType = in.readUint8();
		String identifier = in.readASCIIString(5);
		int volumeDescriptorVersion = in.readUint8();
		in.readUint8();
		
		if(identifier.equals("CD001") == false)
			throw new IOException("unknown identifier " + identifier);
		
		systemIdentifier = in.readASCIIString(32);
		volumeIdentifier = in.readASCIIString(32);
		
		in.readInt32(); // unused
		in.readInt32(); // unused
		
		int volumeSpaceSize = in.readInt32();
		int volumeSpaceSizeBE = in.readInt32BE();

		byte[] skipped32bytes = in.readNBytes(32);
		
		int volumeSetSize = in.readUint16();
		int volumeSetSizeBE = in.readUint16BE();
		int volumeSequenceNumber = in .readUint16();
		int volumeSequenceNumberBE = in .readUint16BE();
		blockSize = in .readUint16();
		int logicalBlockSizeBE = in .readUint16BE();
	
		
		pathTableSize = in.readInt32();
		int pathTableSizeBE = in.readInt32BE();

		if(blockSize != logicalBlockSizeBE)
			throw new IOException(blockSize + " != " + logicalBlockSizeBE);
		if(pathTableSize != pathTableSizeBE)
			throw new IOException(pathTableSize + " != " + pathTableSizeBE);
		
		pathTableLocation = in.readInt32();
		int pathTableOptionalL = in.readInt32();
		int pathTableLocationM = in.readInt32BE();
		int pathTableOptionalM = in.readInt32BE();
		
	}
	
	public List<PathEntry> readPathTable() throws IOException {
		LittleEndianInputStream in = LittleEndianInputStream.wrap(raf);
		List<PathEntry> table = new ArrayList<>();
		
		raf.seek(blockSize * pathTableLocation);

		long pointer = raf.getFilePointer();
		long maxLocation = pathTableSize + pointer;

		while(true) {
			PathEntry p = new PathEntry();
			int idlen = in.readUint8();
			p.extendedAttributeRecordLength = in.readUint8();
			p.locationOfExtent = in.readInt32();
			p.parentNumber = in.readUint16();
			p.identifier = in.readASCIIString(idlen);
			
			if(idlen%2 != 0) {
				in.readUint8(); // padding 0
			}
			
			table.add(p);
			int index = table.size();
			
			p.path = p.identifier;
			
			if(p.parentNumber < index) {
				p.path = table.get(p.parentNumber-1).path + "/" + p.identifier;
			}
			
			pointer = raf.getFilePointer();
			if(pointer >= maxLocation)
				break;
		}
		
		return table;
	}
	
	public void readDirEntries(PathEntry path, List<DirEntry> out) throws IOException {
		raf.seek(path.locationOfExtent * blockSize);
		

		int blockCurrent = 0;
		int blockMax = 0;

		DirEntry first = readDirectoryEntry();
		blockMax = first.dataLength / blockSize - 1;
		
		while(true) {
			DirEntry entry = readDirectoryEntry();
			if(entry == null) {
				if(blockCurrent < blockMax) {
					blockCurrent++;
					raf.seek((path.locationOfExtent+blockCurrent) * blockSize);
					continue;
				}
				break;
			}
			out.add(entry);
		}
	}
	
	
	private DirEntry readDirectoryEntry() throws IOException {
		int recordLength = in.readUint8();
		
		if(recordLength == 0) {
			return null;
		}

		DirEntry d = new DirEntry();
		d.extendedAttributeRecordLength = in.readUint8();
		d.locationOfExtent = in.readInt32();
		if(d.locationOfExtent != in.readInt32BE())
			throw new RuntimeException("sizes are wrong");
		d.dataLength = in.readInt32();
		if(d.dataLength != in.readInt32BE())
			throw new RuntimeException("sizes are wrong");
		in.read(d.date);
		d.flags = in.readUint8();
		d.fileUnitSize = in.readUint8();
		d.interleaveGap = in.readUint8();
		d.volumeSequenceNumber = in.readUint16();
		if(d.volumeSequenceNumber != in.readUint16BE())
			throw new RuntimeException("sizes are wrong");
		
		int idlen = in.readUint8();
		d.fileIdentifier = in.readASCIIString(idlen);
		int read = 33 + idlen;
		if(idlen%2==0) {
			in.readUint8(); // skip to even
			read++;
		}
		
		// system use that we don't need
		if(read<recordLength) {
			in.skip(recordLength-read);
		}
		
		return d;
	}
	
	public static class PathEntry {
		String path;
		String identifier;
		int parentNumber;
		int locationOfExtent;
		int extendedAttributeRecordLength;
	}
	
	public static class DirEntry {
		String fileIdentifier;
		int extendedAttributeRecordLength;
		int locationOfExtent;
		int dataLength;
		final byte[] date = new byte[7];
		int flags;
		int fileUnitSize;
		int interleaveGap;
		int volumeSequenceNumber;
		
		@Override
		public String toString() {
			return "DirEntry [ name=" + fileIdentifier + ", flags=" + String.format("%8s", Integer.toBinaryString(flags)).replace(" ", "0")
					+ ", dataLength=" + dataLength + "]";
		}

		public boolean isNotFinal() {
			return (flags & 0b10000000) != 0;
		}
		
		public boolean isDirectory() {
			return (flags & 0b10) != 0;
		}
	}
}
