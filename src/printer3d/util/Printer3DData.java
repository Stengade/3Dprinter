package printer3d.util;

import printer3d.OpenPrinter3DException;

public class Printer3DData {

	public static Printer3DData getData(String respons, Printer3DReader reader) throws OpenPrinter3DException {
		Printer3DData data = new Printer3DData();
		while(true) {
			//At any point index must be 0
			int pos = respons.indexOf(":");
			String key = respons.substring(0, pos);
			respons = respons.substring(pos+1);
			
			int index = getKeyIndex(respons);
			if(index == -1) {
				put(data, key, respons.trim());
				break;
			}
			
			put(data, key, respons.substring(0, index).trim());
			respons = respons.substring(index);
		}
		
		String line;
		while((line = reader.readLine()) != null) {
			if(line.startsWith("ok"))return data;
			
			//It should alwas start width Cap but not allways
			if(line.startsWith("Cap:"))line = line.substring(4);
			
			int pos = line.indexOf(":");
			if(pos == -1)continue;
			put(data, line.substring(0, pos).trim(), line.substring(pos+1).trim());
		}
		
		throw new OpenPrinter3DException("Failed to read printer stats");
	}

	private String firmaware_name;
	private String machine_type;
	private String uuid;

	private static void put(Printer3DData data, String key, String value) {
		switch(key) {
		case "FIRMWARE_NAME":
			data.firmaware_name = value;
		break;
		case "MACHINE_TYPE":
			data.machine_type = value;
		break;
		case "UUID":
			data.uuid = value;
		break;
		}
	}

	private static int getKeyIndex(String respons) {
		int i=0;
		int start = 0;
		while(i<respons.length()) {
			start = i;
			int c = respons.charAt(i++);
			if(isValidKey(c)){
				while(i<respons.length() && isValidKey(respons.charAt(i)))i++;
				if(i < respons.length() && respons.charAt(i) == ':') {
					return start;
				}
			}
		}
		return -1;
	}

	private static boolean isValidKey(int c) {
		return c >= 'A' && c <= 'Z' || c == '_';
	}

	public String getMachineType() {
		return this.machine_type;
	}

	public String getFimware() {
		return this.firmaware_name;
	}

	public String getUuid() {
		return this.uuid;
	}
}
