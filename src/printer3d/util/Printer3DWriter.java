package printer3d.util;

import com.fazecast.jSerialComm.SerialPort;

public class Printer3DWriter {
	private SerialPort port;
	
	public Printer3DWriter(SerialPort port) {
		this.port = port;
	}

	public boolean writeLine(String command) {
		if(!this.port.isOpen())return false;
		byte[] buffer = (command.trim()+"\r\n").getBytes();
		return this.port.writeBytes(buffer, buffer.length, 0) > 0;
	}

}
