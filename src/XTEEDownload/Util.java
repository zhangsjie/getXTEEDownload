package XTEEDownload;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

public class Util {

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

		//String from = "ffaapi.dev@hpe.com";
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", "smtp3.hp.com");
		properties.setProperty("mail.smtp.sendpartial", "true");
		Session session = Session.getDefaultInstance(properties);

		try {
			MimeMessage message = new MimeMessage(session);
			Multipart multipart = new MimeMultipart("alternative");

			message.setFrom(new InternetAddress(from));
			message.setRecipients(Message.RecipientType.TO, toList);
			// if(ccList != "")
			// message.setRecipients(Message.RecipientType.CC, ccList);

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

}