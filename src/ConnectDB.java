
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;

import java.util.Locale;
import java.util.Properties;
// import java.util.logging.Level;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class ConnectDB {
	private static String CONNECTION = null;
	private static String URL_BASIC = null;
	private static String DB = null;
	private static String oldDB = null;
	private static String ID = null;
	private static String PASSWD = null;
	private static String dbPort = null;
	private static String rdsIP = null;

	public static void setDBConnection() throws IOException {

		String dir = System.getProperty("user.dir")+ File.separator;;

		String propFileName = dir.replace("src", "") + File.separator + "bootstrap" + File.separator + "config"
				+ File.separator + "db.properties";
		System.out.println(propFileName);

		try {
			Properties prop = new Properties();

			try (FileInputStream stream = new FileInputStream(new File(propFileName))) {
				prop.load(stream);
			} catch (Exception e) {
				System.out.println(e);
				System.out.println(e);
				return;
			}

			// get the property value and print it out
			ID = prop.getProperty("ID");
			PASSWD = prop.getProperty("PASSWD");
			rdsIP = prop.getProperty("rdsIP");
			dbPort = prop.getProperty("dbPort");

			URL_BASIC = "jdbc:mysql://" + rdsIP + ":" + dbPort + "";
			System.out.println(ID + PASSWD + "    URL_BASIC=" + URL_BASIC);

		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} finally {

		}

	}

	public static void setDBName() {
		try {
			setDBConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Date myDate = new Date(System.currentTimeMillis());
		DB = "db" + dateFormat.format(myDate);

		Calendar cal = Calendar.getInstance();
		cal.setTime(myDate);
		cal.add(Calendar.DATE, -7);
		oldDB = "db" + dateFormat.format(cal.getTime());

		if (AnalysisCT.DEBUG) {
			DB = "test" + cal.get(Calendar.YEAR) + "_" + String.format("%02d", cal.get(Calendar.WEEK_OF_YEAR));
			oldDB = "test" + cal.get(Calendar.YEAR) + "_" + String.format("%02d", cal.get(Calendar.WEEK_OF_YEAR) - 1);
		}
		System.out.println("-------------- DB setted :   " + DB);
	//	AnalysisCT.LOG.log(Level.INFO, "-------------- DB setted :   " + DB);
	}

	public static void setupConnection() {

		if (DB == null) {
			setDBName();
		}
		CONNECTION = URL_BASIC + "/" + DB + "?&useSSL=false&autoReconnect=true&verifyServerCertificate=false";
//		if (AnalysisCT.DEBUG)
//			System.out.println(CONNECTION);
	}

	public static boolean isUpdated() {
		int oldDbSize = 0;
		int newDbSize = 0;

		if (CONNECTION == null)
			setupConnection();

		try {

			Connection conn = DriverManager.getConnection(CONNECTION, ID, PASSWD);
			Statement stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT COUNT(*) from clinicaltrial");
			if (rs.next()) {
				newDbSize = rs.getInt(1);
			}

			rs = stmt.executeQuery("SELECT COUNT(*) FROM " + oldDB + ".clinicaltrial");

			if (rs.next()) {
				oldDbSize = rs.getInt(1);
			}
			rs.close();
			stmt.close();

		} catch (SQLException e) {
			if (newDbSize > 0)
				return true; // error 가 나는 경우 중 DB가 없는 경우, 무조건 update 한다.
			else
				return false; // newDBSize =0 이므로 변하지 않는다.
		}
		return newDbSize > oldDbSize;
	}

	public static void writeModel(ClinicalTrialModel aModel) throws SQLException {

		if (CONNECTION == null)
			setupConnection();

//		if (AnalysisCT.DEBUG)
//			System.out.println(CONNECTION);
		Connection conn = DriverManager.getConnection(CONNECTION, ID, PASSWD);
		String query = "INSERT INTO clinicaltrial (no, applicant, approval_date, product, title, "
				+ "phase, progress, component, comparator,placebo,target_disease,gender,"
				+ "total_subject, num_total_subject, num_domestic_subject,type,sites) VALUES (?,?,?, ?,?, ?,?,?,?,?,?,?,?,?,?,?,?)";

		PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

		ps.setInt(1, aModel.getNo());
		ps.setString(2, aModel.getApplicant());
		ps.setDate(3, (Date) aModel.getApprovalDate());
		ps.setString(4, aModel.getProduct());
		ps.setString(5, aModel.getTitle());
		ps.setString(6, aModel.getPhase());
		ps.setString(7, aModel.getProgress());
		ps.setString(8, aModel.getComponent());
		ps.setString(9, aModel.getComparator());
		ps.setString(10, aModel.getPlacebo());

		ps.setString(11, aModel.getTargetDisease());
		ps.setString(12, aModel.getGender());
		ps.setString(13, aModel.getTotalSubject());
		if (aModel.getNumTotalSubject() != null)
			ps.setInt(14, aModel.getNumTotalSubject());
		else
			ps.setNull(14, java.sql.Types.INTEGER);
		if (aModel.getNumDomesticSubject() != null)
			ps.setInt(15, aModel.getNumDomesticSubject());
		else
			ps.setNull(15, java.sql.Types.INTEGER);
		ps.setString(16, aModel.getType());
		ps.setString(17, aModel.getSites());
		ps.executeUpdate();

		ResultSet rs = ps.getGeneratedKeys();
		int lastInsertId = 0; // ct_id
		if (rs.next()) {
			lastInsertId = rs.getInt(1);
		}
//System.out.println("aModel.getSites===="+ aModel.getSites() );

		query = "INSERT INTO site_ct (f_ct_id, f_sref_id, site_name, site_name_org,address, location, correct_date,site_order, matched_lead_site) VALUES(?,?,?,?,?,?,?,?,?)";

		for (SiteCTModel sctm : aModel.getSctList()) {
			ps = conn.prepareStatement(query);
			ps.setInt(1, lastInsertId);
			if (sctm.getSrefId() != null)
				ps.setInt(2, sctm.getSrefId());
			else
				ps.setNull(2, java.sql.Types.INTEGER);
			ps.setString(3, sctm.getSiteName());

			ps.setString(4, sctm.getSiteNameOrg());
			ps.setString(5, sctm.getAddress());
			ps.setString(6, sctm.getLocation());
			ps.setDate(7, (Date) sctm.getCorrectDate());
			ps.setInt(8, sctm.getSiteOrder());
			ps.setInt(9, sctm.getMatchedLeadSite());
			ps.executeUpdate();
//System.out.println(sctm.getSiteName());
		}

		ps.close();
		conn.close();

	}

	/* send a query to write on DB */
	public static int sendWriteQuery(String query) throws SQLException {
		//if (AnalysisCT.DEBUG)
		//	System.out.println(" received query=" + query);
		if (CONNECTION == null)
			setupConnection();
		// System.out.println("db="+DB);
		Connection conn = DriverManager.getConnection(CONNECTION, ID, PASSWD);
		Statement stmt = conn.createStatement();

		int result = stmt.executeUpdate(query);
		// if (AnalysisCT.DEBUG)
//			System.out.println(" result = "+ result + " from " + query);

		stmt.close();
		conn.close();
		return result;

	}

	/*
	 * 목적: create DB and tables, and upload data on site table 방법: 1. create db
	 * mysql_create.sql 2. upload "임상시험실시기관지정현황_20180115_keyword.csv" on "site"
	 * table *
	 */

	public static void createDb() throws SQLException, IOException {

		setDBName();
		CONNECTION = URL_BASIC + "/?useSSL=false";
		sendWriteQuery("DROP SCHEMA IF EXISTS " + DB);

		sendWriteQuery("CREATE SCHEMA IF NOT EXISTS " + DB + " DEFAULT CHARACTER SET utf8");
		// sendWriteQuery("USE " + DB);
		CONNECTION = URL_BASIC + "/" + DB + "?verifyServerCertificate=false&autoReconnect=true&useSSL=false";

		sendWriteQuery("DROP TABLE IF EXISTS `" + DB + "`.`site_ref` ");
		sendWriteQuery("CREATE TABLE IF NOT EXISTS `" + DB + "`.`site_ref` ("
				+ " `sref_id` INT NOT NULL AUTO_INCREMENT, " + " `site_name_ref` VARCHAR(20) NOT NULL, "
				+ " `tag1` VARCHAR(30) NULL, " + " `tag2` VARCHAR(20) NULL, " + " `address_ref` VARCHAR(100) NULL, "
				+ "  `approval_type` VARCHAR(6) NULL, " + " UNIQUE INDEX `site_name_UNIQUE` (`site_name_ref` ASC),"
				+ " PRIMARY KEY (`sref_id`)) " + " ENGINE = InnoDB");
		sendWriteQuery("DROP TABLE IF EXISTS `" + DB + "`.`clinicaltrial` ");
		sendWriteQuery("CREATE TABLE  IF NOT EXISTS `" + DB + "`.`clinicaltrial` ( "
				+ " `ct_id` INT NOT NULL AUTO_INCREMENT, " + " `no` INT NULL," + " `applicant` VARCHAR("
				+ ClinicalTrialModel.LEN_APPLICANT + ") NULL, " + " `approval_date` DATE NULL,  "
				+ " `product` VARCHAR(" + ClinicalTrialModel.LEN_PRODUCT + ") NULL,  " + " `title` VARCHAR("
				+ ClinicalTrialModel.LEN_TITLE + ") NULL, " + " `phase` VARCHAR(" + ClinicalTrialModel.LEN_PHASE
				+ ") NULL,  " + " `progress` VARCHAR(3) NULL,  " + " `component` VARCHAR("
				+ ClinicalTrialModel.LEN_COMPONENT + ") NULL,  " + " `comparator` VARCHAR("
				+ ClinicalTrialModel.LEN_COMPARATOR + ") NULL,  " + " `placebo` VARCHAR("
				+ ClinicalTrialModel.LEN_PLACEBO + ") NULL,  " + " `target_disease` VARCHAR("
				+ ClinicalTrialModel.LEN_TARGET_DISEASE + ") NULL,  " + " `gender` VARCHAR("
				+ ClinicalTrialModel.LEN_GENDER + ") NULL,  " + " `total_subject` VARCHAR("
				+ ClinicalTrialModel.LEN_TOTAL_SUBJECT + ") NULL," + " `num_total_subject` INT NULL, "
				+ " `num_domestic_subject` INT NULL, " + " `type` VARCHAR(" + ClinicalTrialModel.LEN_TYPE + ") NULL, "
				+ " `sites` VARCHAR(" + ClinicalTrialModel.LEN_SITES + ") NULL, "

				+ " `cmt` VARCHAR(1000) NULL,  " + " PRIMARY KEY (`ct_id`)) " + " ENGINE = InnoDB");

		sendWriteQuery("DROP TABLE IF EXISTS `" + DB + "`.`site_ct` ");
		sendWriteQuery("CREATE TABLE IF NOT EXISTS `" + DB + "`.`site_ct` (" + " `sct_id` INT NOT NULL AUTO_INCREMENT, "
				+ " `f_ct_id` INT NOT NULL, " + "  `site_name` VARCHAR(" + SiteCTModel.LEN_SITE_NAME + ")  NULL,"
				+ " `site_name_org` VARCHAR(" + SiteCTModel.LEN_SITE_NAME_ORG + ") NULL," + " `f_sref_id` INT NULL, "
				+ " `address` VARCHAR(" + SiteCTModel.LEN_ADDRESS + ") NULL," + " `location` VARCHAR(2) NULL,"
				+ "`site_order` INT NOT NULL," + "`matched_lead_site` INT DEFAULT 0," + " `correct_date` DATE NULL,"
				+ " PRIMARY KEY (`sct_id`), " + "  INDEX `fk_site_ct_clinicaltrial1_idx` (`f_ct_id` ASC),"
				+ "  INDEX `fk_site_ct_site_ref1_idx` (`f_sref_id` ASC)," + " CONSTRAINT `fk_site_ct_clinicaltrial1`"
				+ "    FOREIGN KEY (`f_ct_id`)" + "    REFERENCES `" + DB + "`.`clinicaltrial` (`ct_id`)"
				+ "    ON DELETE NO ACTION" + "    ON UPDATE NO ACTION," + "  CONSTRAINT `fk_site_ct_site_ref1`"
				+ "    FOREIGN KEY (`f_sref_id`)" + "    REFERENCES `" + DB + "`.`site_ref` (`sref_id`)"
				+ "    ON DELETE NO ACTION" + "    ON UPDATE NO ACTION" + ")" + " ENGINE = InnoDB");
		sendWriteQuery("ALTER TABLE site_ct ADD UNIQUE `site_ct_index_id`(`f_ct_id`, `site_name`)");
		createViewTables();
	}

	public static void createViewTables() throws SQLException {
		CONNECTION = URL_BASIC + "/" + DB
				+ "?verifyServerCertificate=false&autoReconnect=true&useSSL=false&sessionVariables=group_concat_max_len=204800";
		sendWriteQuery(
				"CREATE VIEW VIEW_CT_SITECT_ALL AS select  ct.ct_id ,s.site_order ,s.site_name, ct.title, ct.phase, ct.type, ct.approval_date, s.location from clinicaltrial  as ct left join site_ct as s  on ct.ct_id=s.f_ct_id where s.site_name is not null");
		sendWriteQuery(
				"CREATE VIEW VIEW_CT_SITECT_LEAD AS select ct.ct_id ,s.site_name, ct.title, ct.phase, ct.type, ct.approval_date, s.location from clinicaltrial  as ct left join site_ct as s on ct.ct_id=s.f_ct_id where s.site_name is not null and s.matched_lead_site=1 ");
		
	}

	public static void exportExcelFile(String path) throws SQLException {
		if (CONNECTION == null)
			setupConnection();
		Connection con = DriverManager.getConnection(CONNECTION, ID, PASSWD);
		Statement stmt = con.createStatement();

		Workbook writeWorkbook = new HSSFWorkbook();
		Sheet desSheet = writeWorkbook.createSheet("new sheet");

		ResultSet rs = null;
		try {
			File exportFile = new File(path + "clinicaltrialkr.xls");
			FileOutputStream fileOut = new FileOutputStream(exportFile);

			String query = "SELECT ct_id as no, approval_date as approval, applicant, product, title, phase, sites FROM clinicaltrial ";

			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();

			Row desRow1 = desSheet.createRow(0);
			for (int col = 0; col < columnsNumber; col++) {

				Cell newpath = desRow1.createCell(col);
				newpath.setCellValue(rsmd.getColumnLabel(col + 1));
			}


			while (rs.next()) {
	//System.out.println("Row number" + rs.getRow());
				Row desRow = desSheet.createRow(rs.getRow());
				for (int col = 0; col < columnsNumber; col++) {
					Cell newpath = desRow.createCell(col);
					newpath.setCellValue(rs.getString(col + 1));
				}
				// if(stop++==10) break;

			}
			writeWorkbook.write(fileOut);
			fileOut.close();
			writeWorkbook.close();
			System.out.println(exportFile.getAbsoluteFile());
		//	AnalysisCT.LOG.log(Level.INFO, "File is exported as " + exportFile.getAbsoluteFile());

		} catch (SQLException | IOException e) {
			System.out.println("Failed to get data from database");

			// TODO Auto-generated catch block
			e.printStackTrace();
			//AnalysisCT.LOG.log(Level.INFO, "[SEVERE]Failed to get data from database");
		}

	}


	/* 임상시험 실시기관 지정 현황 update */
	public static void main(String[] args) {
		try {

			setDBConnection();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void writeConfigFile(String fileName) throws FileNotFoundException, IOException {

		if (AnalysisCT.DEBUG)
			System.out.println("filename=" + fileName);
		LocalDateTime currentTime = LocalDateTime.now();
		System.out.println("Current DateTime: " + currentTime);

		FileWriter fw = new FileWriter(fileName, false);
		StringBuilder sb = new StringBuilder();
		sb.append("host =" + rdsIP + "\n");
		sb.append("user = " + ID + "\n");
		sb.append("pwd = " + PASSWD + "\n");
		sb.append("port= 3306\n");
		sb.append("dbname = " + DB + "\n");
		// sb.append("update = " +LocalDate.now() + "\n");
		fw.write(sb.toString());
		fw.close();

		//AnalysisCT.LOG.log(Level.INFO, "--------------db. ini is written  with DB name=  " + DB);

	}

}
