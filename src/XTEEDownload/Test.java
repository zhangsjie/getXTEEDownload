package XTEEDownload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

public class Test {
	private static Properties prop;
	private static HashMap<String,String> SNmap=new HashMap<String,String>();
	public static void main(String[] args) {

		try {
			InputStream inputStream = new FileInputStream(args[0]);
			prop = new Properties();
			prop.loadFromXML(inputStream);

			String serialNumber = null;
			BufferedReader SerialNumberReader = null;
			String line = null;
			StringBuilder sb = new StringBuilder("");

			File SerialNumberFile = new File(prop.getProperty("SerialNumberFile") + "serialNumberFile.txt");

			SerialNumberReader = new BufferedReader(new FileReader(SerialNumberFile));
			while ((line = SerialNumberReader.readLine()) != null) {
				String line0 = line.split(" ")[0];
				if(line0.length()==0 ){
					continue;
				}
				String line1=line.split(" ").length==1?" ":line.split(" ")[1];
				SNmap.put(line0, line1);
				System.out.println(line0);
				sb.append(line0 + ",");
			}
			String sn = sb.toString().trim();
			serialNumber = sn.substring(0, sn.length() - 1);
			System.out.println(serialNumber);
			System.out.println(SNmap.toString());
			SerialNumberReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}