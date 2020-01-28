import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {
	public static String validate(String str, int limitChar) {
		if( str==null || str.equals("")) return null;
		
		if(str.length()>limitChar) {
		//AnalysisCT.LOG.log(AnalysisCT.LOG.getLevel(), "[too long] " +  "\t [value]=" +  str);
			return str.substring(0, limitChar);
		}
		else return str;		
	}
	public static java.sql.Date convertToSQLDate(String sdate) throws Exception {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date date = formatter.parse(sdate);
		java.sql.Date sqlDate = new java.sql.Date(date.getTime());
		return sqlDate;
	}
}
