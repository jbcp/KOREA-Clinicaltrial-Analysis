import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SiteRef {
	/*
	 * 담당부서 | 임상제도과
	 * 공고번호 제2019-011호
	 * 약사법 제34조2에 따른 의약품 임상시험 실시기관을 붙임과 같이 공고합니다.
	 * 
	 * link: http://www.mfds.go.kr/brd/m_76/list.do
	 * keyword: 	임상시험 실시기관
	 * 이 기록 외에도 생동성으로 허가 받은 실시기관도 포함 (매뉴얼로 추가) 하여 approval_type 열에 생동으로 표기
	 */
	private final String SITEFILE = "임상시험실시기관지정현황.csv";

	private static  String[] refSiteNameArr ;
	private static  HashMap<String, Integer>   refTagHM = new HashMap<String, Integer>(); ; //sitename, srefId

	public SiteRef() {	
	 
	}
	public void debugPrint() {
		
		for (int i=0; i< refSiteNameArr.length;i++ ) {
			System.out.println(i+"\t"+ refSiteNameArr[i]);
		}
		 List<Entry<String, Integer>> list = new ArrayList<>(refTagHM.entrySet());
	        list.sort(Entry.comparingByValue());

	        Map<String, Integer> sortedMap = new LinkedHashMap<>();
	        for (Entry<String, Integer> entry : list) {
	        	sortedMap.put(entry.getKey(), entry.getValue());
	        }
	        System.out.println(Arrays.asList(sortedMap)); // method 1
	        System.out.println(Collections.singletonList(sortedMap)); // method 2
	} 
		
		  
			
	
	public static void main(String[] args) {
		SiteRef sr= new SiteRef();
		try {
			String query =sr.getSiteRefDataInsertQuery();
			System.out.println("siteRef=="+query);
			ConnectDB.sendWriteQuery(query);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String[] getRefSiteNameArr() {
		return refSiteNameArr;
	}

	public HashMap<String, Integer> getRefTagHM() {
		return refTagHM;
	}
    public static int getSrefId(String siteName) { 
    	Integer srefId=refTagHM.get(siteName);
    	
    	if(srefId==null) {    		
    		return -1;
    	}
    	else return srefId;
    }
    public static String getSiteNameRef(int srefId) {
    	int lengthArr=refSiteNameArr.length;
    	if(srefId<=0 || srefId>=lengthArr) {
    		return null;
    	}
    	else return refSiteNameArr[srefId];
    }
    
	public  String getSiteRefDataInsertQuery() throws Exception, IOException {
		return getInsertQueryfromCSV(SITEFILE, "site_ref");// save data of reference file on site_ref table
		//ConnectDB.sendWriteQuery(getSiteRefDataInsertQuery() );
	}

	/*
	 * CSV 파일을 DB에 저장하는 쿼리를 반환한다. 첫줄(header)을 읽어 insert 할 컬럼 쿼리를 만들고 그 외 모든 행은 데이터로
	 * 저장되어 모든 데이터를 insert하는 query 를 반환한다.
	 * 
	 * 임상시험실시지관지정현황.csv 내용-> 첫줄은 header=[site_name,tag1,tag2,address]
	 * 
	 * 여기서, site_name 칼럼의 데이터는  각 srefSiteNameArr[sref_id] 에 저장하고
	 * tag1, tag2 칼럼은 refTagHM<tag1/tag2, sref_id>에 저장한다. 각 임상시험에 해당하는 실시기관 목록을 
	 * 데이터베이스에 저장할때 각 실시기관 이름이 ref 에 있는지 매번 mysql db 에 연결하지 않아도 된다.
	 * 여기서 tag1 은 승인받은 기관의 정식이름인 site_name에서 스페이스를 제거한 값이고, tag2 는 자주쓰이는 닉네임이다.
	 * 따라서 refTagHM 에서 이름을 key로 hit 하면 sref_id 를 value로 리턴받아서  srefSiteNameArr[sref_id]으로
	 * 각 실시기관의 정식이름을 찾을 수 있다. 즉, 실시기관 명칭을 표준화한다.
	 *   
	 */
	private  String getInsertQueryfromCSV(String filename, String TableName) throws IOException {
		final String content = readFile(filename);
		final String[] lines = content.split("\n");

//		if (AnalysisCT.DEBUG)
//			System.out.println("read and save to site table --" + filename);

		if (lines.length < 2)
			throw new IOException(filename + " has no data");

		
		refSiteNameArr = new String[lines.length];
		
		final StringBuilder sb = new StringBuilder();

		String header = lines[0].trim().substring(1);//csv에서 첫캐릭터를 읽을때 xml 의 구분자가  첨가된듯.
	
		String[] headerArr = header.split(",");
		//header = header.substring(header.indexOf(",")+1); // 첫칼럼은 sref_id  auto increase 이므로 insert 에서 제외
		
		int numCol = headerArr.length;
		
		sb.append("INSERT INTO " + TableName + "(" + header + ") VALUES ");

		for (int i = 1; i < lines.length; i++) { //i=sref_id
			final String[] parts = lines[i].trim().split(",");

			sb.append("(");
			
			
			for (int k = 0; k < numCol; k++) {
				String str	;
				
				if (k >= parts.length) {
					str = "";
				} 
				else {
					str= parts[k].trim();
				
					if (k == 0) //"site_name" column 
						refSiteNameArr[i]=str; // orignal site name 을 array 로 저장하여 ref로 삼는다.  
					else if(k==1 || k==2) {// "tag1" or "tag2" column
						refTagHM.put(str, i);
						//if(i==lines.length-1) System.out.println(str +" \t"+i);
					}					
				}
				
			
				if (str.equals(""))
					sb.append("NULL");
				else
					sb.append("'" + str + "'");

				if (k != numCol - 1)
					sb.append(", ");
			}

			sb.append(")");
			if (i != lines.length - 1)
				sb.append(", ");
		}
		sb.append("");

		if (AnalysisCT.DEBUG)
			System.out.println("[save SiteRef data query / SiteRef.java]  = "+sb.toString());

		//System.out.println((lines.length-1)+"refsiteName "+ refSiteNameArr[lines.length-1]);

		//System.out.println(		refTagHM.size());
		
		return sb.toString();
	}

	public  String readFile(String filename) throws IOException {
		final File file = new File(filename);
		final FileInputStream fis = new FileInputStream(file);
		final byte[] data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();
		return new String(data, "UTF-8");
	}

	// /*
	// * 먼저 key1 에서 매치값을 찾아 맞으면 해당
	// *
	// * error check (입력값->수정값 in 승인목록) 1.오기: 고산대학교복음병원 ->고신대학교복음병원 2. 대전대학교대전한방병원
	// * ->대전대학교 둔산한방병원 3. 중앙대학교의과대학부속용산병원 ->중앙대학교병원 !!! 모두 반영 ( key2 칼럼)
	// *
	// *
	// * 그외 두번째 칼럼(key2)에서 매치됨 1. 을지병원 ->을지대학교 을지병원 2. 한림대학교 강동성심병원 ->성심의료재단 강동성심병원
	// 3.
	// * 원광대학교병원 ->원광대학교의과대학병원 4. 강동경희대학교병원->강동경희대학교의대병원
	// *
	// * 없는 경우 NULL 처리 1. 의료법인 예성의료재단 베데스다병원 2. 학교법인 인하병원
	// */
	// public static String getMatchedSite(String search) throws SQLException {
	//
	// String oneSite = sendReadQueryforOwnAnswer(search, "key1");
	// if (oneSite == null)
	// oneSite = sendReadQueryforOwnAnswer(search, "key2");
	//
	// if (oneSite == null) {
	// System.out.println(" no matched of " + search + " key1 and key2");
	// return null;
	// } else
	// return oneSite;
	// }
	//
	// /* site 테이블에서 입력되는ㄴ column에서 search와 매치하는 값의 행을 찾아 "siteName"열의 값을 반환한다. */
	// public static String sendReadQueryforOwnAnswer(String search, String column)
	// throws SQLException {
	// if (CONNECTION == null)
	// setupConnection();
	// String query = "select * from site where " + column + " like '" + search +
	// "'" + "OR key1 like '" + search
	// + "'";
	//
	// Connection conn = DriverManager.getConnection(CONNECTION, ID, PASSWD);
	// Statement stmt = conn.createStatement();
	//
	// String firstAnswer = "";
	// ResultSet rs = stmt.executeQuery(query);
	// int size = 0;
	// if (rs.last()) {
	// size = rs.getRow();
	// rs.beforeFirst();
	// }
	//
	// while (rs.next()) {
	// firstAnswer = rs.getString(2);// Retrieve by column site_name
	// if (size > 1) {
	// for (int i = 1; i <= 5; i++) {
	// System.out.print(rs.getString(i));
	// System.out.print("\t ");
	// }
	// System.out.println("");
	// throw new SQLException("over 1 data matched of " + search + " with query =" +
	// query);
	// }
	// }
	//
	// rs.close();
	//
	// stmt.close();
	// conn.close();
	//
	// return firstAnswer;
	// }

}
