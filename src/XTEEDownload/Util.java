package XTEEDownload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.swing.JOptionPane;

import org.omg.CORBA.PUBLIC_MEMBER;

public class Util {
	private static String MESSAGE = "";

	public static Boolean WriteToDB(String query) throws NamingException, SQLException {
		InitialContext ic = new InitialContext();
		DataSource ds = (DataSource) ic.lookup("java:comp/env/jdbc/IC_CLQR");
		Connection c = ds.getConnection();
		Statement stmt = c.createStatement();
		if (stmt.executeUpdate(query) == 1) {
			c.close();
			return true;
		}

		c.close();
		return false;
	}

	public static void SendEMail(String subject, String toList, String ccList, String body, String from)
			throws IOException, SQLException, ClassNotFoundException {

		// String from = "ffaapi.dev@hpe.com";
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", "smtp3.hp.com");
		properties.setProperty("mail.smtp.sendpartial", "true");
		Session session = Session.getDefaultInstance(properties);

		try {
			MimeMessage message = new MimeMessage(session);
			Multipart multipart = new MimeMultipart("alternative");

			message.setFrom(new InternetAddress(from));
			message.setRecipients(Message.RecipientType.TO, toList);
			if (ccList != "" && ccList != null) {
				message.setRecipients(Message.RecipientType.CC, ccList);
			}
			message.setSubject(subject);
			MimeBodyPart bodyPart = new MimeBodyPart();
			bodyPart.setContent(GetHTMLBody(body), "text/html");
			multipart.addBodyPart(bodyPart);
			message.setContent(multipart);

			Transport.send(message);

		} catch (MessagingException mex) {
			System.out.println(mex.getCause());
			mex.printStackTrace();
		}
	}

	private static String GetHTMLBody(String message) {
		return "<span style=\"font-family:Arial;font-size:10pt;\">Dear,<br><br>" + message
				+ "<br><br><b>Thanks,<br>CLQR Group</b></span>";
	}

	public static String TruncateField(String field, int length) {
		if (field.length() > length) {
			String a = field.substring(0, length);
			return a;
		}
		return field;
	}

	public static String GetVendorName(String fileName) {
		String[] arr;
		arr = fileName.split("[_.]");
		if (arr.length == 11) {
			return arr[1];
		} else {
			return "";
		}
	}

	public static String listMap(Map<String, String> map) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : map.entrySet()) {
			sb.append(entry.getKey() + " : ");
			sb.append(entry.getValue() + ", ");
			sb.append("<br />");
		}
		String str = sb.toString();
		return str;

	}

	public static HashMap<String, String> addValue(HashMap<String, String> map, String key, String value) {
		if (map.containsKey(key)) {
			map.put(key, map.get(key) + value);
		} else {
			map.put(key, value + ",");
		}
		return map;
	}

	public static String replaceFileds(String recivedData) {
		recivedData = recivedData.replace("MQD_LOCATION", "MANUFACTURING_OPERATION_NAME");
		recivedData = recivedData.replace("TAGNO", "TAG_NO");
		recivedData = recivedData.replace("DIAGNAME", "DIAG_TEST");
		recivedData = recivedData.replace("RECVDATE", "RECV_DATE");
		recivedData = recivedData.replace("REPAIRCODE", "REPAIR_CODE");
		recivedData = recivedData.replace("PARTOBSERVATION", "PART_OBSERVATION");
		recivedData = recivedData.replace("FAILUREFREQUENCY", "FAILURE_FREQUENCY");
		recivedData = recivedData.replace("FAILEDASSEMBLY", "FAILED_ASSEMBLY");
		recivedData = recivedData.replace("FAILEDSERIAL", "FAILED_SERIAL");
		recivedData = recivedData.replace("REPLACEDASSEMBLY", "REPLACED_ASSEMBLY");
		recivedData = recivedData.replace("REPLACEDSERIAL", "REPLACED_SERIAL");
		recivedData = recivedData.replace("PARTLOCATION", "PART_LOCATION");
		recivedData = recivedData.replace("SECONDPART", "SECOND_PART");
		recivedData = recivedData.replace("SECONDSERIAL", "SECOND_SERIAL");
		recivedData = recivedData.replace("SECONDACTION", "SECOND_ACTION");
		recivedData = recivedData.replace("TIME_2_FAIL", "TIME_TO_FAIL");
		recivedData = recivedData.replace("FAULT_CODE_CAT", "NC_CODE_DESC");
		recivedData = recivedData.replace("FAULT_CODE", "NC_CODE");
		recivedData = recivedData.replace("MASTER_WO", "MASTER_FACTORYWORKOBJECT");
		recivedData = recivedData.replace("CMSTARREVISION", "TEST_PLAN_VERSION");
		recivedData = recivedData.replace("DIAG_NAME", "DIAG_TEST");
		recivedData = recivedData.replace("FAIL_SECT", "FAIL_SUBTEST");
		recivedData = recivedData.replace("FAIL_LOOP_TX", "FAIL_LOOP");
		recivedData = recivedData.replace("PLN_LOOP_TX", "TEST_PLAN_VERSION");
		recivedData = recivedData.replace("SEED_VALU_TX", "SEED_VALUE");
		recivedData = recivedData.replace("EDC", "ACTN_PART_SRL_RVIS");
		recivedData = recivedData.replace("WEEKMFG", "ACTN_PART_DT_CD");
		recivedData = recivedData.replace("DIAG_TX", "DIAG_CONDITIONS");
		return recivedData;
	}

	public static boolean copyFile(String srcFileName, String destFileName, boolean overlay) {
		File srcFile = new File(srcFileName);

		// 判断源文件是否存在
		if (!srcFile.exists()) {
			MESSAGE = "源文件：" + srcFileName + "不存在！";
			JOptionPane.showMessageDialog(null, MESSAGE);
			return false;
		} else if (!srcFile.isFile()) {
			MESSAGE = "复制文件失败，源文件：" + srcFileName + "不是一个文件！";
			JOptionPane.showMessageDialog(null, MESSAGE);
			return false;
		}

		// 判断目标文件是否存在
		File destFile = new File(destFileName);
		if (destFile.exists()) {
			// 如果目标文件存在并允许覆盖
			if (overlay) {
				// 删除已经存在的目标文件，无论目标文件是目录还是单个文件
				new File(destFileName).delete();
			}
		} else {
			// 如果目标文件所在目录不存在，则创建目录
			if (!destFile.getParentFile().exists()) {
				// 目标文件所在目录不存在
				if (!destFile.getParentFile().mkdirs()) {
					// 复制文件失败：创建目标文件所在目录失败
					return false;
				}
			}
		}

		// 复制文件
		int byteread = 0; // 读取的字节数
		InputStream in = null;
		OutputStream out = null;

		try {
			in = new FileInputStream(srcFile);
			out = new FileOutputStream(destFile);
			byte[] buffer = new byte[1024];

			while ((byteread = in.read(buffer)) != -1) {
				out.write(buffer, 0, byteread);
			}
			return true;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				if (out != null)
					out.close();
				if (in != null)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static HashMap<String, String> filterMap(HashMap<String, String> map0, HashMap<String, String> map1) {
		HashMap<String, String> map2 = new HashMap<String, String>();
		for (String key0 : map0.keySet()) {
			boolean ishavekey = false;

			for (String key1 : map1.keySet()) {
				if (key0 == key1) {
					ishavekey = true;
				}
			}
			if (ishavekey) {
				map2.put(key0, map0.get(key0) == null ? "" : map0.get(key0));
			} else {
				List<String> values0 = new ArrayList<String>(Arrays.asList(map0.get(key0).split(",")));
				List<String> values1 = new ArrayList<String>(Arrays.asList(map1.get(key0).split(",")));
				
				if (map0.get(key0) == null || values1.size() == 0) {
					map2.put(key0, map0.get(key0) == null ? "" : map0.get(key0));
				} else {
					for (String value : values0) {
						if (map1.get(key0).indexOf(value) == -1) {
							addValue(map2, key0, value);
						}
					}
				}

			}
		}

		return map2;

	}
}
