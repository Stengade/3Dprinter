package printer3d;

import com.fazecast.jSerialComm.SerialPort;

import gcode.GCodeParser;
import printer3d.util.Printer3DData;
import printer3d.util.Printer3DReader;
import printer3d.util.Printer3DWriter;

public class Printer3D {
	private static final int[] baudrate = new int[] {
			500000,
			250000,
			230400,
			115200
	};

	public static Printer3D open(@SuppressWarnings("exports") SerialPort port) throws OpenPrinter3DException {
		

		Printer3DWriter writer = new Printer3DWriter(port);
		Printer3DReader reader = new Printer3DReader(port);
		
		for(int i=0;i<baudrate.length;i++) {
			if(!port.openPort())throw new OpenPrinter3DException("Failed to open");
			if(!port.setBaudRate(baudrate[i]))continue;
			String text = tryPrinter(writer, reader);
			if(text == null) {
				port.closePort();
				continue;
			}

			return new Printer3D(port, Printer3DData.getData(text, reader), writer, reader);
		}
		
		throw new OpenPrinter3DException("No corect respons from device");
	}
	
	private static String tryPrinter(Printer3DWriter writer, Printer3DReader reader) {
		
		if(!writer.writeLine("M115"))return null;
		
		String respons = reader.readLine();
		
		//The buffer can have noise in it so the printer dont know the command. 
		//Read if the printer return unknown command and try again
		if(respons != null && respons.startsWith("echo:Unknown command:")) {
			reader.readLine();
			if(!writer.writeLine("M115"))return null;
			respons = reader.readLine();
		}
		
		if(respons == null || !respons.startsWith("FIRMWARE_NAME:"))return null;
		return respons;
	}

	private Printer3DData data;
	private SerialPort port;
	private Printer3DWriter writer;
	private Printer3DReader reader;
	
	private boolean is_absolute_position = false;
	private int target_bed_tempratur = -1;
	private int target_nozzle_tempratur = -1;
	private int printer_x_pos = -1;
	private int printer_y_pos = -1;
	private int printer_z_pos = -1;
	
	private Printer3D(SerialPort port, Printer3DData data, Printer3DWriter writer, Printer3DReader reader) {
		this.data = data;
		this.port = port;
		this.writer = writer;
		this.reader = reader;
		
		//disable timeout (Wee dont know how longe the operation will take)
		this.reader.disableTimer();
	}

	public int boudrate() {
		return this.port.getBaudRate();
	}

	public String getMachineType() {
		return this.data.getMachineType();
	}

	public boolean autoHome() {
		return this.autoHome(false);
	}
	
	public boolean autoHome(boolean waitFinish) {
		this.writer.writeLine("G28");
		if(waitFinish) {
			if(!this.checkResultRespons())
				return false;

			return this.waitFinishMove();
		}
		return this.checkResultRespons();
	}
	
	/**
	 * Wait for the current movement is done
	 * @return true on success else false
	 */
	private boolean waitFinishMove() {
		this.writer.writeLine("M400");
		return this.checkResultRespons();
	}

	public void close() {
		this.port.flushIOBuffers();
		this.port.closePort();
	}

	/**
	 * Display text on printer. 
	 * Current it is only reprap fimware there support it?
	 * @param message The message you want to send to
	 * @return true on success else false
	 */
	public boolean displayText(String message) {
		this.writer.writeLine("M117 "+message);
		return this.checkResultRespons();
	}

	/**
	 * Send a command to tell the printer to make a sound.
	 * @param miliseconds Sounds length in miliseconds
	 * @return When printer send ok it return true
	 */
	public boolean beep(int miliseconds) {
		this.writer.writeLine("M300 S"+miliseconds+" P1000");
		return this.checkResultRespons();
	}
	
	
	public boolean isMarlin() {
		return this.data.getFimware().indexOf("Marlin") != -1;
	}
	
	public boolean isRepRapFimware() {
		return this.data.getFimware().indexOf("RepRap") != -1;
	}

	public String getFirmwareName() {
		return this.data.getFimware();
	}

	public String getUuid() {
		return this.data.getUuid();
	}
	
	/**
	 * The printer can send respons wee dont want. Here wee skip them and waiting on ok or error
	 * @return True on ok else false
	 */
	private boolean checkResultRespons() {
		while(true) {
			String line = this.reader.readLine();
			if(line == null || line.startsWith("error"))return false;
			if(line.startsWith("ok"))return true;
		}
	}

	public boolean sendCommand(String command) {
		GCodeParser gcode = GCodeParser.parse(command);
		
		switch(gcode.getCommand()) {
		case "G90":
			this.is_absolute_position = true;
		break;
		case "G91":
			this.is_absolute_position = false;
		break;
		case "M104", "M109":
			if(gcode.hasAttribute("S")) {
				try {
					this.target_nozzle_tempratur = Integer.parseInt(gcode.getAttribute("S"));
				}catch(NumberFormatException e) {
					
				}
			}
		
			if(gcode.hasAttribute("R")) {
				try {
					this.target_nozzle_tempratur = Integer.parseInt(gcode.getAttribute("R"));
				}catch(NumberFormatException e) {
				
				}
			}
		break;
		case "M140", "M190":
			if(gcode.hasAttribute("S")) {
				try {
					this.target_bed_tempratur = Integer.parseInt(gcode.getAttribute("S"));
				}catch(NumberFormatException e) {
					
				}
			}
		
			if(gcode.hasAttribute("R")) {
				try {
					this.target_bed_tempratur = Integer.parseInt(gcode.getAttribute("R"));
				}catch(NumberFormatException e) {
				
				}
			}
		break;
		case "G0", "G1", "G2", "G3":
			//wee have move position
			if(gcode.hasAttribute("X")) {
				try {
					int x = Integer.parseInt(gcode.getAttribute("X"));
					if(this.is_absolute_position)
						this.printer_x_pos = x;
					else {
						if(this.printer_x_pos != -1) {
							this.printer_x_pos += x;
						}
					}
				}catch(NumberFormatException e) {
					
				}
			}
		
			if(gcode.hasAttribute("Y")) {
				try {
					int y = Integer.parseInt(gcode.getAttribute("Y"));
					if(this.is_absolute_position)
						this.printer_y_pos = y;
					else {
						if(this.printer_y_pos != -1) {
							this.printer_y_pos += y;
						}
					}
				}catch(NumberFormatException e) {
				
				}
			}
			
			if(gcode.hasAttribute("Z")) {
				try {
					int z = Integer.parseInt(gcode.getAttribute("Z"));
					if(this.is_absolute_position)
						this.printer_z_pos = z;
					else {
						if(this.printer_z_pos != -1) {
							this.printer_z_pos += z;
						}
					}
				}catch(NumberFormatException e) {
				
				}
			}
		break;
		default:
		}
		
		if(!this.writer.writeLine(command))return false;
		return this.checkResultRespons();
	}

	public boolean isConnected() {
		return this.port.isOpen();
	}

	public boolean isAbsolutePosition() {
		return this.is_absolute_position;
	}

	public int getTargetBedTemprature() {
		return this.target_bed_tempratur;
	}
	
	public int getTargetNozzleTemprature() {
		return this.target_nozzle_tempratur;
	}

	public int getXLocation() {
		return this.printer_x_pos;
	}

	public int getYLocation() {
		return this.printer_y_pos;
	}

	public int getZLocation() {
		return this.printer_z_pos;
	}

	public String getPortName() {
		return this.port.getSystemPortName();
	}
}
