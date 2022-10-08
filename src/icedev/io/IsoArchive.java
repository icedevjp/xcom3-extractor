package icedev.io;

import java.io.*;
import java.util.*;

import icedev.io.ISO9660.*;
import icedev.xcom.extract.DecodePCK.DataSupplier;

public class IsoArchive implements Closeable {
	public class IsoEntry {
		public final String name;
		public final long offset;
		public final long length;
		
		public IsoEntry(String name, long offset, long length) {
			this.name = name;
			this.offset = offset;
			this.length = length;
		}
	}
	
	RandomAccessFile raf;
	FileInputStream rin;
	
	Map<String, IsoEntry> entries = new HashMap<>();
	
	public IsoArchive(File iso) throws IOException { 
		raf = new RandomAccessFile(iso, "r");
		rin = new FileInputStream(raf.getFD());
		
		populateEntries();
	}
	
	public IsoEntry get(String path) {
		return entries.get(path);
	}
	
	public InputStream open(String path) {
		return open(get(path));
	}
	
	public InputStream open(IsoEntry entry) {
		return new IsoEntryInputStream(entry.offset, entry.offset+entry.length);
	}
	
	public InputStream open(IsoEntry entry, int innerOffset) {
		if(innerOffset<0)
			throw new IllegalArgumentException("offset must be positive");
		long limit = entry.offset+entry.length;
		return new IsoEntryInputStream(Math.min(innerOffset+entry.offset, limit), limit);
	}
	
	private void populateEntries() throws IOException {
		ISO9660 iso = new ISO9660(raf);
		iso.readVolumeDescriptor();
		List<PathEntry> table = iso.readPathTable();
		List<DirEntry> dirs = new ArrayList<>();
		for(PathEntry p : table) {
			iso.readDirEntries(p, dirs);
			for(DirEntry d : dirs) {
				if(d.isDirectory())
					continue;
				IsoEntry e = new IsoEntry(d.fileIdentifier.split(";")[0], d.locationOfExtent * iso.blockSize, d.dataLength);
				entries.put(p.path + "/" + e.name, e);
			}
			dirs.clear();
		}
	}

	class IsoEntryInputStream extends InputStream {
		long offset;
		long limit;
		
		IsoEntryInputStream(long offset, long limit) {
			this.offset = offset;
			this.limit = limit;
		}

		@Override
		public int read() throws IOException {
			if(offset >= limit)
				return -1;
			raf.seek(offset++);
			return rin.read();
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int max = (int) (limit-offset);
			if(max <= 0)
				return -1;
			raf.seek(offset);
			int read = rin.read(b, off, Math.min(len, max));
			if(read > 0)
				offset += read;
			return read;
		}
		
		@Override
		public int available() throws IOException {
			int max = (int) (limit-offset);
			raf.seek(offset);
			int avail = rin.available();
			return Math.min(max, avail);
		}
		
		@Override
		public long skip(long n) throws IOException {
			int max = (int) (limit-offset);
			long skipped = Math.min(n, max);
			offset += skipped;
			return skipped;
		}
	}
	
	public void close() throws IOException {
		raf.close();
	}
}
