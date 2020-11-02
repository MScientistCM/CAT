/**
Copyright (C) 2020  John Locke

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;

//int rgb565 = (((red & 0b11111000)<<8) + ((green & 0b11111100)<<3) + (blue>>3));

//https://champman0102.co.uk/showthread.php?t=14406

//https://stackoverflow.com/questions/3842828/converting-little-endian-to-big-endian

//009660F8     14             DB 14

public class ColouredAttributeTool {

	private static final String GPL =
			"Copyright (C) 2020  John Locke\r\n" + 
			"\r\n" + 
			"This program is free software: you can redistribute it and/or modify\r\n" + 
			"it under the terms of the GNU General Public License as published by\r\n" + 
			"the Free Software Foundation, either version 3 of the License, or\r\n" + 
			"(at your option) any later version.\r\n" + 
			"\r\n" + 
			"This program is distributed in the hope that it will be useful,\r\n" + 
			"but WITHOUT ANY WARRANTY; without even the implied warranty of\r\n" + 
			"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\r\n" + 
			"GNU General Public License for more details.\r\n" + 
			"\r\n" + 
			"You should have received a copy of the GNU General Public License\r\n" + 
			"along with this program.  If not, see <http://www.gnu.org/licenses/>.";
	
	
	private static final int DEFAULT_OFFSET = 0x5660F6;
	
	public static void main(String[] args) throws IOException {
		
		System.out.println(GPL);
		
		File x11File = new File("x11.txt");
		
		File configFile = new File("config.txt");
		
		Map<String, String> config = readConfigFile(configFile, "=");
		Map<String, String> x11 = readConfigFile(x11File, "#");
		
		String dirPath = config.get("directory");
		String strOffset = config.get("offset");
		
		strOffset = strOffset.replaceAll("0x", "");
		
		final int OFFSET = strOffset == null ? DEFAULT_OFFSET : Integer.parseInt(strOffset, 16);
		
		if(OFFSET != DEFAULT_OFFSET) {
			System.out.println("Unexpected offset: " + OFFSET);
			System.out.println("I hope you know what you're doing! :-)");
		}
		
		final JFileChooser fc = new JFileChooser();		
		
		if(dirPath != null) {
			File dir = new File(dirPath);
			fc.setCurrentDirectory(dir);
		}
		
		fc.setDialogTitle("Select your CM executable");
		
		
		
		
		int code = fc.showOpenDialog(null);

		if(code == JFileChooser.CANCEL_OPTION) {
			System.out.println("Cancel clicked, closing.");
			return;
		} else if(code == JFileChooser.ERROR_OPTION) {
			System.out.println("Selection aborted, closing.");
			return;
		}
		
		File exe = fc.getSelectedFile();
		if(exe == null || exe.isDirectory()) {
			System.out.println("Tool cannot work with " + exe);
			return;
		}
		
		//File exe = new File("NickColourTest.exe");
		
		File cf = new File("colours.txt");
		
		Map<Integer, Color> map = readColours(cf, x11);
		
		RandomAccessFile raf = new RandomAccessFile(exe, "rw");
		
		int count = 0;
		
		for(Integer key : map.keySet()) {
			if(key != null && key > 0) {
				raf.seek(OFFSET + (key * 2));
				
				Color color = map.get(key);

				if(color == null) {
					System.err.println("Oops. Colour not found: " + key);
					continue;
				}
				int rgb888 = color.getRGB();
				
				int rgb565 = (((rgb888&0xf80000)>>8) + ((rgb888&0xfc00)>>5) + ((rgb888&0xf8)>>3));
				
				String big = (Integer.toHexString(rgb565) + "000");;
				if(big.length() == 6) {
					big = "0" + big;
				} else if(big.length() == 5) {
					big = "00" + big;
				}
				
				big = big.substring(0, 4);
								
				//convert to Little Endian
				raf.write(Integer.parseInt(big.substring(2, 4), 16));
				raf.write(Integer.parseInt(big.substring(0, 2), 16));

				count++;
				
			}
			
		}	
		
		raf.close();
		
		System.out.println(count + " colours changed.");
		System.out.println("Done.");
		
	}

	private static Map<String, String> readConfigFile(File f, String splitChar) throws IOException {
		Map<String, String> map = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line= "";
		
		while ((line = br.readLine()) != null) {
			if(line.startsWith("//") || line.trim().length() == 0 || ! line.contains(splitChar)) {
				continue;
			}
			String[] tmp = line.split(splitChar, 2);
			map.put(tmp[0].trim().toLowerCase(), tmp[1].trim().toLowerCase());
		}
		br.close();
		return map;
	}

	private static Map<Integer, Color> readColours(File f, Map<String, String> x11) throws IOException {
		Map<Integer, Color> map = new HashMap<Integer, Color>();
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line= "";
		
		while ((line = br.readLine()) != null) {
			if(line.startsWith("//") || line.trim().length() == 0 || ! line.contains("=")) {
				continue;
			}
			
			String[] tmp = line.split("=", 2);
			
			Integer key = Integer.parseInt(tmp[0].trim());
			
			String strColour = tmp[1].trim().toLowerCase();
			String x11Value = x11.get(strColour);
			
			Color c = null;
			if(x11Value != null) {
				c = Color.decode("0x" + x11Value.toLowerCase());
			} else {
				if(! strColour.contains("0x")) {
					strColour = "0x" + strColour;
				}
				try {
					c = Color.decode(strColour);
				} catch(NumberFormatException nfe) {
					System.err.println("Error: '" + strColour + "' ");
					
				}
				
			}
						
			map.put(key, c);
		}
		br.close();
		
		return map;
	}
	
}
