package XTEEDownload;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.apache.logging.log4j.*;

import XTEEDownload.Util;;

public class XTEE {
	private static final Logger LOGGER = LogManager.getLogger(XTEE.class);
	private static Properties prop;
	private static List<String> SNNotFound = new ArrayList<String>();
	private static HashMap<String, String> SNmap = new HashMap<String, String>();
	private static HashMap<String, String> SNmapFound = new HashMap<String, String>();
	private static HashMap<String, String> SNmapNotFound = new HashMap<String, String>();
	private static HashSet<String> siteSet = new HashSet<String>();

	public static void main(String[] args) {

		System.out.println("starting process at: " + new Date());
		Date start = new Date();
		if (args.length < 1) {
			System.out.println("No config file argument provided!");
			return;
		}
		try {
			LOGGER.log(Level.INFO, "loading config file");
			InputStream inputStream = new FileInputStream(args[0]);
			prop = new Properties();
			prop.loadFromXML(inputStream);
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage() + "can not loading config file");
			return;
		}

		String JsonURL = "";
		ArrayList<String> regions = new ArrayList<String>(Arrays.asList(prop.getProperty("regions").split(",")));

		int fileCounter = 0;
		if (buildSerialNumberList() != null) {
			regions.remove(0);
			regions.remove(0);
		}
		for (String region : regions) {
			LOGGER.log(Level.INFO, "Start processing : " + region);
			JsonURL = buildUrl(region);

			try {
				fileCounter += jsonCall(JsonURL, "event", prop.getProperty("tempLocation"),
						prop.getProperty("outputLocation"), region);
			} catch (Exception e) {
				LOGGER.log(Level.ERROR, "error at jsonCall: " + e.getMessage() + e.getStackTrace());
				// LOGGER.log(Level.ALL, e.getStackTrace());
			}
		}
		LOGGER.log(Level.INFO, "fileCounter: " + fileCounter);
		if (fileCounter >= 1) {
			// write out updated dateFile
			try {
				LOGGER.log(Level.INFO, "writing dateFile");
				DateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss", Locale.ENGLISH);
				FileWriter fw = new FileWriter(prop.getProperty("dateFile") + ".tmp");
				fw.write(format.format(start));
				fw.close();
				File f = new File(prop.getProperty("dateFile"));
				if (f.delete()) {
					(new File(prop.getProperty("dateFile") + ".tmp")).renameTo(f);
				}
				if (siteSet.size() > 0) {
					sendEmailForSite(siteSet);
					updateSitefile(siteSet);

				}
			} catch (Exception e) {
				LOGGER.log(Level.ERROR, "error writing dateFile" + e.getMessage());
			}
		}
		if (JsonURL.indexOf("serialnumber") != -1) {
			// send email
			if (fileCounter == 0) {
				SNmapNotFound = SNmap;
			}

			sendEmailToUser(buildSerialNumberList());
			LOGGER.log(Level.INFO, "sendEmail done");
		}
		// System.exit(0);
	}

	private static void updateSitefile(HashSet<String> set) {
		BufferedWriter MQDFileWriter = null;
		try {
			prop.getProperty("MQDFileLocation");
			File MQDFile = new File(prop.getProperty("MQDFileLocation") + "MQDGuideEmailSites.txt");
			MQDFileWriter = new BufferedWriter(new FileWriter(MQDFile, true));
			for (String s : set) {
				MQDFileWriter.write(s + ",N/A,N/A,N/A,N/A,N" + "\n");
			}
			MQDFileWriter.close();
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, "Error updateSitefile" + e.getMessage());
		}
	}

	/**
	 * 
	 * send email to ashok for the new site
	 * 
	 * @author zhangshe
	 * @param set
	 * 
	 */
	private static void sendEmailForSite(HashSet<String> set) {
		String fromEmail = prop.getProperty("fromEmail");
		String subject = "ATTENTION: New FFA Site Records Received ***DO NOT REPLY***";
		String toList = prop.getProperty("toEmail");
		String ccList = prop.getProperty("ccEmail");

		String body = "";
		int i = 1;
		StringBuilder siteList = new StringBuilder();
		for (String s : set) {
			siteList.append(i++ + "." + s + "<br />");
		}

		body = "<br />" + siteList.toString() + "<br />";
		try {
			Util.SendEMailForSite(subject, toList, ccList, body, fromEmail);
		} catch (ClassNotFoundException e) {
			LOGGER.log(Level.ERROR, "ClassNotFoundException" + e.getMessage());
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, "IOException" + e.getMessage());
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "error on send Email ForSite" + e.getMessage());
		}
	}

	/**
	 * send emil to user who use this jar for the serialNumber
	 * 
	 * @author zhangshe
	 * @param serialNumber
	 */
	private static void sendEmailToUser(String serialNumber) {
		String fromEmail = prop.getProperty("fromEmail");
		String subject = "The serial number's analysis for the records of Xtee";
		String toList = prop.getProperty("toEmail");
		String ccList = prop.getProperty("ccEmail");

		String body = "";
		String line0 = "The following serial numbers are all of you want to query:<br />----" + serialNumber + "<br />";
		String line1 = "XTEE Recon Process has completed download of following serial numbers:<br />";
		String line3 = "The following serial numbers were not found in XTEE API:<br />";
		StringBuilder line2 = new StringBuilder("----");
		StringBuilder line4 = new StringBuilder("----");
		List<String> SNList = new ArrayList<String>(Arrays.asList(serialNumber.split(",")));

		for (String record : SNNotFound) {
			Iterator<String> it = SNList.iterator();
			while (it.hasNext()) {
				String sn = it.next();
				if (record.indexOf(sn) != -1) {
					it.remove();
				}
			}
		}

		for (String s : SNList) {
			line2.append(s + ",");
		}
		line2.deleteCharAt(line2.length() - 1);
		line2.append("<br />");

		for (String s : SNNotFound) {
			line4.append(s + ",");
		}
		line4.deleteCharAt(line4.length() - 1);
		line4.append("<br />");
		body = line0 + line1 + line2.toString() + line3 + line4.toString();
		// body = line0 + line1 + Util.listMap(SNmapFound) + line3 +
		// Util.listMap(SNmapNotFound);
		try {

			Util.SendEMail(subject, toList, ccList, body, fromEmail);
			LOGGER.log(Level.INFO, "serialNumberlist: " + SNmap.toString());
		} catch (ClassNotFoundException e) {
			LOGGER.log(Level.ERROR, "ClassNotFoundException" + e.getMessage());
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, "IOException" + e.getMessage());
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "error on sendEmail" + e.getMessage());
		}
		changeNameForSerialNumberFile();

	}

	private static void changeNameForSerialNumberFile() {
		// 我要获取当前的日期
		Date date = new Date();
		// 设置要获取到什么样的时间
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH-mm-ss");
		// 获取String类型的时间
		String createdate = sdf.format(date);
		String srcFileName = prop.getProperty("SerialNumberFile") + "serialNumberFile.txt";
		String destFileName = prop.getProperty("SerialNumberFile") + createdate + "_serialnumberFile.txt";
		Util.copyFile(srcFileName, destFileName, true);

	}

	private static int writeXTEEDataToFile(String jsonURL, String folder, List<String> myList, String inputFolder,
			String fileType, String region, Date start) throws IOException, JSONException {
		// HashMap<String, String> MQDMap = new HashMap<String, String>();
		if (jsonURL.indexOf("serialnumber") != -1) {
			getSerialNumberInfo(buildSerialNumberList(), myList);
			Iterator<String> it = myList.iterator();
			while (it.hasNext()) {
				String x = it.next();
				for (String key : SNmap.keySet()) {
					ArrayList<String> reference = new ArrayList<String>(Arrays.asList(SNmap.get(key).split(",")));
					if (reference == null || reference.size() == 0) {
						continue;
					}
					boolean ishaverefence = false;
					for (String s : reference) {
						if (x.indexOf(s) != -1) {
							ishaverefence = true;
						}
					}
					if (!ishaverefence) {
						it.remove();
					}
				}

			}

		}
		/*
		 * if (jsonURL.indexOf("serialnumber") == -1) { myList =
		 * validationMQDSites(myList); }
		 */
		/*
		 * if (region == "EMEA"){ LOGGER.log(Level.INFO,
		 * "1111111111111111111111111111"); }
		 */
		String headers = jsonURL.split("fields=")[1];
		String[] headerArray = headers.split(",");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS", Locale.ENGLISH);

		File dir = (new File(folder));
		dir.mkdirs();
		File file = new File(folder + "\\output.csv");

		Calendar cal1 = Calendar.getInstance();
		String month = cal1.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH).substring(0, 3);
		String dateForFileName = month + "_" + cal1.get(Calendar.DAY_OF_MONTH) + "_" + cal1.get(Calendar.YEAR) + "_"
				+ cal1.get(Calendar.HOUR_OF_DAY) + "H_" + cal1.get(Calendar.MINUTE) + "m_" + cal1.get(Calendar.SECOND)
				+ "s";
		StringBuilder pathName = new StringBuilder();

		pathName = new StringBuilder(
				inputFolder + "IIPGSITIWS.Xtee" + region.replace("%20", "") + "_TXT_EVENT_" + dateForFileName + ".csv");

		BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
		// LOGGER.log(Level.INFO, "beginning to write file for region " +
		// region);
		// 判断当前系统是否为windows
		// String osName="unix";
		// if( System.getProperty("os.name").toLowerCase().startsWith("win")){
		// osName="windows";
		// }

		bw.write(headers + "\n");
		for (String str : myList) {

			String FILE_NAME = str.substring(str.indexOf("FILE_NAME") + 12, str.indexOf("FILE_CREATE_DT") - 3);
			String FILE_CREATE_DT = str.substring(str.indexOf("FILE_CREATE_DT") + 17, str.indexOf("FILE_INFO") - 3);
			String sublist = str.substring(str.indexOf("ACTIONCODE"), str.indexOf("Links") - 3);
			// LOGGER.log(Level.INFO, "sublist:" + sublist);
			List<String> splitList = new ArrayList<String>(Arrays.asList(sublist.split("\\\\n")));
			Map<String, String> map = new LinkedHashMap<String, String>();
			for (String head : headerArray) {
				map.put(head, "");
			}

			for (String split : splitList) {
				int a = split.indexOf(":");
				int b = split.indexOf(":", a + 1);
				String sub1 = split.substring(0, a).trim();
				String sub2 = split.substring(a + 1, b).trim();
				String sub_3 = split.substring(b + 1, split.length()).trim();
				String sub3 = sub_3.replaceAll(",", ".");

				for (String key : map.keySet()) {
					if (sub1.equalsIgnoreCase(key)) {
						map.put(key, map.get(sub1) + sub3);
					}
				}

			}

			// 加入RECV_DATE和FILE_NAME
			for (String key : map.keySet()) {
				if (key.equals("FILE_NAME")) {
					map.put(key, FILE_NAME);
				}
				if (key.equals("RECV_DATE")) {
					map.put(key, FILE_CREATE_DT.replace("T", " "));
				}
				if (key.equals("SRC_SYS_NM")) {
					map.put(key, "FFA_API_XTEE");
				}
			}
			if (jsonURL.indexOf("serialnumber") == -1) {

				String site = "guide2_" + map.get("SITE") + ",";

				if (!validationMQDSites(site)) {
					continue;
				}
				// myList = validationMQDSites(myList);
			}

			// LOGGER.log(Level.INFO, "map:" + map.toString());
			for (String key : map.keySet()) {
				// LOGGER.log(Level.INFO, value);
				bw.write(map.get(key) + ",");
			}
			bw.write("\n");

		}
		bw.close();
		LOGGER.log(Level.INFO, pathName.toString());
		File destinationFile = new File(pathName.toString());
		try {
			// LOGGER.log(Level.INFO, "File written Begin for: " + region);
			file.renameTo(destinationFile);

			LOGGER.log(Level.INFO, "File written successfully for: " + region);

			return 1;
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			LOGGER.log(Level.ERROR, e.getMessage() + " ===---=== " + errors.toString());
			return 0;
		}
	}

	/**
	 * 方法作用:记录serialnumber NotFound.
	 * 
	 * @author zhangshe
	 * 
	 * @param serialNumber
	 * @param myList
	 */
	/**
	 * @param serialNumber
	 * @param myList
	 */
	public static void getSerialNumberInfo(String serialNumber, List<String> myList) {
		List<String> SNList = new ArrayList<String>(Arrays.asList(serialNumber.split(",")));

		for (String record : myList) {
			Iterator<String> it = SNList.iterator();
			while (it.hasNext()) {
				String sn = it.next();
				if (record.indexOf(sn) != -1) {
					it.remove();

				}
			}
		}
		SNNotFound = SNList;
		/*
		 * for (String sn : SNmap.keySet()) { boolean ishaveSN = false; for
		 * (String record1 : myList) { if (record1.indexOf(sn) != -1) { ishaveSN
		 * = true; String reference = SNmap.get(sn); if (reference == null ||
		 * reference == "") { SNmapFound.put(sn, ""); } else { List<String>
		 * referenceSplit = new
		 * ArrayList<String>(Arrays.asList(reference.split(","))); // boolean
		 * ishaverefer = false; for (String refer : referenceSplit) { if
		 * (record1.indexOf(refer) != -1) { // ishaverefer = true;
		 * Util.addValue(SNmapFound, sn, refer); } } } } }
		 * 
		 * } SNmapNotFound = Util.filterMap(SNmap, SNmapFound);
		 */
	}

	public static int jsonCall(String jsonURL, String fileType, String folder, String inputFolder, String region)
			throws IOException, JSONException {
		int success = 0;
		SSLContext sslcontext = null;
		try {
			try {
				sslcontext = getSSLContext();
			} catch (KeyManagementException e) {
				LOGGER.log(Level.ERROR, "error creating sslcontext" + e.getMessage());
			}
		} catch (NoSuchAlgorithmException e1) {
			LOGGER.log(Level.ERROR, "error creating sslcontext" + e1.getMessage());
		}

		DefaultClientConfig config = new DefaultClientConfig();
		Map<String, Object> properties = config.getProperties();
		HTTPSProperties httpsProperties = new HTTPSProperties((new HostnameVerifier() {
			@Override
			public boolean verify(String s, SSLSession sslSession) {
				return true;
			}
		}), sslcontext);
		properties.put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, httpsProperties);
		Date start = new Date();
		Client client = Client.create(config);
		client.addFilter(new LoggingFilter());
		String jsonURL1 = jsonURL.split("fields=")[0];
		String jsonURL2 = jsonURL1.substring(0, jsonURL1.length() - 1);
		LOGGER.log(Level.INFO, "sending URL request" + jsonURL2);
		WebResource service = client.resource(jsonURL2);
		String recivedData = null;
		recivedData = service.get(String.class);
		recivedData = recivedData.substring(1, recivedData.length() - 1);

		List<String> myList = new ArrayList<String>(Arrays.asList(Util.replaceFileds(recivedData).split("\\},\\{\"")));

		success = writeXTEEDataToFile(jsonURL, folder, myList, inputFolder, fileType, region, start);

		return success;
	}

	private static SSLContext getSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
		final SSLContext sslContext = SSLContext.getInstance("SSL");

		// set up a TrustManager that trusts everything
		sslContext.init(null, new TrustManager[] { new X509TrustManager() {
			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} }, new SecureRandom());

		return sslContext;
	}

	public static String buildUrl(String region) {
		String tempURL = "";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		BufferedReader dateReader = null;

		try {
			File dateFile = new File(prop.getProperty("dateFile"));
			dateReader = new BufferedReader(new FileReader(dateFile));
			String date = dateReader.readLine();
			dateReader.close();
			String serialNumber = buildSerialNumberList();
			if (date.length() > 0) {
				Calendar cal = Calendar.getInstance();
				Date from = format.parse(date);
				cal.setTime(from);
				cal.add(Calendar.HOUR, -12);
				from = cal.getTime();
				date = format.format(from).replace(" ", "%20");
				// tempURL = prop.getProperty("URL") +
				// "2016-11-23&to=2016-11-30&region=EMEA" + "&" +
				// https://dss-iis01.americas.hpqcorp.net/api/dsr/xteencffile?from=2017-05-06&to=2017-05-30&region=Americas
				if (serialNumber != null) {
					tempURL = prop.getProperty("SNURL") + "serialnumber=" + serialNumber + "&"
							+ prop.getProperty("fields");
				} else {
					tempURL = prop.getProperty("URL") + date + "&to=" + format.format(new Date()).replace(" ", "%20")
							+ "&region=" + region + "&" + prop.getProperty("fields");
				}

			}

		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "Error building URL:" + e.getMessage());
		}
		return tempURL;
	}

	/**
	 * 
	 * @return
	 */
	public static String buildSerialNumberList() {
		String serialNumber = null;
		BufferedReader SerialNumberReader = null;
		String line = null;
		StringBuilder sb = new StringBuilder("");
		try {
			File SerialNumberFile = new File(prop.getProperty("SerialNumberFile") + "serialNumberFile.txt");
			if (!SerialNumberFile.exists() || SerialNumberFile.length() == 0) {
				return null;
			}
			SerialNumberReader = new BufferedReader(new FileReader(SerialNumberFile));
			while ((line = SerialNumberReader.readLine()) != null) {
				String line0 = line.split(" ")[0];
				if (line0.length() == 0) {
					continue;
				}
				String line1 = line.split(" ").length == 1 ? " " : line.split(" ")[1];
				SNmap.put(line0, line1);
				sb.append(line0 + ",");
			}
			String sn = sb.toString().trim();
			serialNumber = sn.substring(0, sn.length() - 1);
			SerialNumberReader.close();
		} catch (FileNotFoundException e) {
			LOGGER.log(Level.ERROR, "Error Build SerialNumber: FileNotFound" + e.getMessage());
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, "Error Build SerialNumber: IOException" + e.getMessage());
		}
		return serialNumber;
	}

	public static List<String> buildMQDSites() {
		List<String> dataList = new ArrayList<>();
		List<String> MQDList = new ArrayList<>();
		BufferedReader MQDFileReader = null;
		String line = null;
		try {
			prop.getProperty("MQDFileLocation");
			File MQDFile = new File(prop.getProperty("MQDFileLocation") + "MQDGuideEmailSites.txt");
			MQDFileReader = new BufferedReader(new FileReader(MQDFile));
			while ((line = MQDFileReader.readLine()) != null) {
				MQDList.add(line);
			}
			MQDFileReader.close();
			// Iterator<String> it = myList.iterator();
			/*
			 * while (it.hasNext()) { String record = it.next(); int a =
			 * record.indexOf("site:0:"); int b = record.indexOf("\n", a + 1);
			 * String site = "guide2_" + record.substring(a + 7, b) + ",";
			 * LOGGER.log(Level.INFO, "site" + site); boolean YorN = true; for
			 * (String s : MQDList) { if (s.startsWith(site) && s.endsWith("N"))
			 * { YorN = false; } } if (!YorN) { it.remove(); } }
			 * 
			 * dataList = myList;
			 */
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, "Error buildMQDSites" + e.getMessage());
		}
		return MQDList;

	}

	public static boolean validationMQDSites(String site) {
		List<String> MQDList = buildMQDSites();
		boolean YorN = false;
		for (String s : MQDList) {
			if (s.startsWith(site)) {
				if (s.endsWith("Y")) {
					YorN = true;
				}
			} else {
				siteSet.add(site.substring(0,site.length()-1));
			}
		}
		return YorN;

	}

}
