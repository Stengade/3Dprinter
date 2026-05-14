package printer3d.util;

import java.io.IOException;
import java.io.InputStream;

import com.fazecast.jSerialComm.SerialPort;

public class Printer3DReader {
	private InputStream stream;
	private SerialPort port;
	private boolean timerEnabled = true;
	private String lastLine = "";
	
	public Printer3DReader(SerialPort port) {
		this.stream = port.getInputStream();
		this.port = port;
	}

	public String readLine() {
		if(!this.port.isOpen())return null;
		
		StringBuilder builder = new StringBuilder();
		while(true) {
			try {
				long start = System.currentTimeMillis();
				while(this.stream.available() == 0) {
					if(this.timerEnabled && System.currentTimeMillis() - start > 15) {
						if(builder.isEmpty())return null;
						return builder.toString();
					}
				}
				int c = this.stream.read();
				if(c == -1 || c == '\n') {
					this.lastLine = builder.toString().trim();
					return this.lastLine;
				}
				builder.append((char)c);
			} catch (IOException e) {
				return null;
			}
		}
	}
	
	public void close() {
		try {
			this.stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void disableTimer() {
		this.timerEnabled = false;
	}

	public String getLastLine() {
		return this.lastLine;
	}

}
