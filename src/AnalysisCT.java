import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
//test 테스트
/* 목적: 주단위로 BASEURL에 있는 웹사이트에 접속하여 정보를 매번 새로운 database에 저장한다.
 * 
 *  
 * Required library : Jsoup -1.11.3       
 * 2018-6-25 이지형 
 * 
 * 
 * baseurl 이 변경됨 2019-1월 확인.
 */
/* 로직 순서
 * 
 * database set up
 * -> save site reference data from CSV  on "site_ref" table
 * -> crawling (read pages -> read a page->
 *      10 threads save 10 clinicaltrials data on databases tables : "clinicaltrial", "site_ct"
 *      )
 *  -> check previous database size (db_old) and the db whether or not there is updates.
 *  -> if updates, change db_name on db_ini.txt
 *     
 */

public class AnalysisCT {
	public static String BASEURL = "https://nedrug.mfds.go.kr/pbp/CCBBC01";
	// 링크
	public final static boolean DEBUG = false;
	public static String DIR;

//	public final static Logger LOG = Logger.getLogger("AnalysisCT");

	public AnalysisCT() {
		if (DEBUG)
			BASEURL = "https://nedrug.mfds.go.kr/pbp/CCBBC01/getList?totalPages=1&page=1&limit=10&sort=&sortOrder=&searchYn=true&applyEntpName=&approvalDtStart=2012-03-27&approvalDtEnd=2012-03-31&prductName=&clinicStepCode=&clinicExamTitle=&btnSearch=";
	}

	public static void main(String[] args) {
		DIR = System.getProperty("user.dir") + File.separator;
		String logpath = DIR.replace("src", "") + File.separator + "log" + File.separator;
		String configPath = DIR.replace("src", "") + File.separator + "bootstrap" + File.separator + "config"
				+ File.separator;

		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Date myDate = new Date(System.currentTimeMillis());

		try {

			//FileHandler fh = new FileHandler(logpath + "analysisCT.log", false);
			// fh.setFormatter(new SimpleFormatter() {
			// 	public String format(LogRecord log) {

			// 		String logOutput = "[" + (new Date()).toString() + "][" + log.getLevel() + "] MSG[ "
			// 				+ log.getMessage() + "]\n";

			// 		return logOutput;
			// 	}
			// });
			// fh.setEncoding("utf-8");

			// LOG.addHandler(fh);

			//System.out.println("logpath: " + logpath);
		//	System.out.println("configpath:  " + configPath);
			LocalDateTime currentTime = LocalDateTime.now();
		//	System.out.println("Current DateTime: " + currentTime);
			//LOG.log(Level.INFO, "===================Current DateTime: " + currentTime);
		//	LOG.log(Level.INFO, "logpath: " + logpath);
			//LOG.log(Level.INFO, "configpath:  " + configPath);

			AnalysisCT act = new AnalysisCT();
			SiteRef sref = new SiteRef();

			// setup DB setting
			ConnectDB.createDb();

			String query = sref.getSiteRefDataInsertQuery();

			// if (DEBUG) sref.debugPrint();

			ConnectDB.sendWriteQuery(query);

			act.startWebcrawling();

			if (ConnectDB.isUpdated()) {// 중간에 스크래핑이 웹사이트 읽다가 에러나는 경우가 종종 생김: 아마도 응답시간때문인듯
				// .. 따라서 마지막에 총 개수를 파악해서 다시 스크래핑하던지 업데이트 안하는게 좋다.
				ConnectDB.writeConfigFile(configPath + "db.ini");

				ConnectDB.exportExcelFile(DIR.replace("src", "") + File.separator + "bootstrap" + File.separator
						+ "public" + File.separator + "excelfiles" + File.separator);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//LOG.log(Level.SEVERE, e.toString());
		}
	}

	public void startWebcrawling() throws SQLException, Exception {
		Document doc;

		doc = Jsoup.connect(BASEURL).get();

		Elements boardCountEls = doc.getElementsByClass("board_count");
		Elements spanEls = boardCountEls.select("span");
		int totalListNum = Integer.parseInt(spanEls.get(0).text().trim().replaceAll("[^0-9]+", ""));
		int totalPages = (int) Math.ceil(totalListNum / 10.0);

		if (DEBUG)
			System.out.println("total list num=" + totalListNum + "\ttotalPages=" + totalPages);

		//LOG.log(Level.INFO, "total list num=" + totalListNum);
		readAll(totalPages);
	}

	public void readAll(int totalPages) throws SQLException, IOException, Exception {

		for (int i = totalPages; i > 0; i--) {
			String url = BASEURL + "/getList?totalPages=" + totalPages + "&page=" + i + "&limit=10";
			try {
				readAPage(url);// thread로 할수도 있지만 그럴려면 날짜순이 엉망이 될수 있음.
			} catch (IOException e) {

				readAPage(url);
			}
			// if(DEBUG & (i==totalPages-2)) break;
		}

	}

	public void readAPage(String url) throws IOException, SQLException, Exception {
		if (DEBUG)
			System.out.println("[A page link] " + url);

		// 테이블에서 한줄씩 처리해야하므로 tr elements 를 찾는다. tb_list>tbody>tr
		Document doc = Jsoup.connect(url).get();
		Element tableEl = doc.getElementsByClass("tb_list").first();
		Element tbodyEl = tableEl.getElementsByTag("tbody").first();
		Elements trs = tbodyEl.getElementsByTag("tr");

		for (int i = trs.size() - 1; i >= 0; i--) { // 웹 사이트가 역순이므로 tr을 마지막에서부터 저장해야한다.
			Element tr = trs.get(i);

			saveRaw(tr, url);

		}

	}

	/*
	 * 목적: tb_list 의 하나의 행에서 다음 데이터를 추출하여 DB에 저장 1. "신청자" 열 link 따라 접속하여 한번더 크롤링한다.
	 * 2. "실시기관명" 열의 데이터를 array 로 저장한다. 3. 모델을 이용하여 DB에 저장한다.
	 *
	 * sites data 오류 ( tag2 칼럼 에 반영) 1. 오기: 고산대학교복음병원 ->고신대학교복음병원 2. 대전대학교대전한방병원
	 * ->대전대학교 둔산한방병원 3. 중앙대학교의과대학부속용산병원 ->중앙대학교병원 4. 을지병원 ->을지대학교 을지병원 5. 한림대학교
	 * 강동성심병원 ->성심의료재단 강동성심병원 6. 원광대학교병원 ->원광대학교의과대학병원 7. 강동경희대학교병원->강동경희대학교의대병원
	 * 
	 * 없는 경우 NULL 처리 1. 의료법인 예성의료재단 베데스다병원 2. 학교법인 인하병원
	 */
	public void saveRaw(Element trforaCT, String url) {
		String aref = null;
		try {
			ClinicalTrialModel ctModel = new ClinicalTrialModel();// DataModel
			Elements tdsforaCT = trforaCT.getElementsByTag("td");
			String no = tdsforaCT.get(0).text().trim();
			ctModel.setNo(Integer.parseInt(no));

			Element link = trforaCT.getElementsByTag("a").first(); // "신청자" 열의 link 값이다.

			aref = link.attr("abs:href");
//			if (DEBUG)
//	System.out.println("\t[detail link] " + aref);// detail link
//		
			String sites = tdsforaCT.get(5).getElementsByTag("span").get(1).text().trim();// 실시기관명

//System.out.println("site===" + sites);

			// 위에서 구한 "신청자"link에 접속하여 데이터를 동일 DB에 저장한다.

			Document detailDoc = Jsoup.connect(aref).get();

			Elements detailTbodys = detailDoc.getElementsByTag("tbody");

			Element applicationInfoTbody = detailTbodys.get(0);// 임상시험 신청정보
			Elements applicationInfoTds = applicationInfoTbody.getElementsByTag("td");// 임상시험 신청정보

			if (applicationInfoTds.size() != 3) {
				for (int i = 0; i < applicationInfoTds.size(); i++) {
					String s = applicationInfoTds.get(i).text().trim();
					System.out.println(i + "==== " + s + "   lnegth=" + s.length());
				}
				throw new ReadCustomException("ERROR : detail website 임상시험 신청정보  3!= " + applicationInfoTds.size()
						+ "  on " + aref + "  on " + url);
			}
			ctModel.setTitle(applicationInfoTds.get(0).text().trim());

			ctModel.setApplicant(applicationInfoTds.get(1).text().trim());

			ctModel.setApprovalDate(applicationInfoTds.get(2).text().trim());

			Element drugInfoTbody = detailTbodys.get(1);// 임상시험의약품 기본정보
			Elements drugInfoTds = drugInfoTbody.getElementsByTag("td");// 임상시험의약품 기본정보
			if (drugInfoTds.size() != 4) {
				for (int i = 0; i < drugInfoTds.size(); i++) {
					String s = drugInfoTds.get(i).text().trim();
					System.out.println(i + "==== " + s + "   lnegth=" + s.length());

				}
				throw new ReadCustomException("ERROR : detail website 임상시험 신청정보  3!= " + applicationInfoTds.size()
						+ "  on " + aref + "  on " + url);

			}
			ctModel.setProduct(drugInfoTds.get(0).text().trim());
			ctModel.setComponent(drugInfoTds.get(1).text().trim());
			ctModel.setComparator(drugInfoTds.get(2).text().trim());
			ctModel.setPlacebo(drugInfoTds.get(3).text().trim());

			Element ctInfoTbody = detailTbodys.get(2);// 임상시험 개요
			Elements ctInfoTds = ctInfoTbody.getElementsByTag("td");// 임상시험 개요
			if (ctInfoTds.size() != 5) {
				for (int i = 0; i < ctInfoTds.size(); i++) {
					String s = drugInfoTds.get(i).text().trim();
					System.out.println(i + "==== " + s + "   lnegth=" + s.length());

				}
				throw new ReadCustomException("ERROR : detail website  임상시험 개요 5!= " + applicationInfoTds.size()
						+ "  on " + aref + "  on " + url);
			}
			ctModel.setTargetDisease(ctInfoTds.get(0).text().trim());
			ctModel.setPhase(ctInfoTds.get(1).text().trim());
			ctModel.setType(ctInfoTds.get(2).text().trim());
			ctModel.setGender(ctInfoTds.get(3).text().trim());
			String totalsubject = ctInfoTds.get(4).text().trim();
			ctModel.setTotalSubject(totalsubject);
			// extract number to set setNumTotalSubject() and setNumDomesticSubject()

			if (totalsubject != null && !totalsubject.equals("")) { // 빈공란.

				totalsubject = totalsubject.replaceAll(",", "");
				int wholeIndex = totalsubject.indexOf("전체");
				int domesticIndex = totalsubject.indexOf("국내");
				if (wholeIndex >= 0 && domesticIndex >= 0) { // 전체 국내 동시

					String whole = totalsubject.substring(0, domesticIndex - 1);
					String domestic = totalsubject.substring(domesticIndex);
					ctModel.setNumTotalSubject(extractOneNum(whole));
					ctModel.setNumDomesticSubject(extractOneNum(domestic));

				} else if (domesticIndex >= 0) { // 국내만

					ctModel.setNumDomesticSubject(extractOneNum(totalsubject));
				} else if (wholeIndex >= 0) { // 전체만

					ctModel.setNumTotalSubject(extractOneNum(totalsubject));
				} else { // unknwon error
					System.out.println(
							"error doing extract num_total_subject and num_domestic_subject. it is not one number "
									+ totalsubject + " on " + aref + "  on  " + url);

					//Log.log(Level.WARNING,
							//"error doing extract num_total_subject and num_domestic_subject.  it is not one number "
									//+ totalsubject + " on " + aref + "  on  " + url);

				}
			}
			Element siteInfoTbody = detailTbodys.get(3);// 임상시험 실시기관 정보
			Elements trs = siteInfoTbody.getElementsByTag("tr");

//System.out.println("------------------trs size=" + trs.size());

			int order = 1;
			int lead = 1;
			for (int i = 0; i < trs.size(); i++) {

				Element tr = trs.get(i);
				Elements spans = tr.getElementsByTag("span");
				if (spans.size() < 6)// 머리글만 있는 경우 size==1
					continue; // 한줄 기관 정보의 spans.size()==6 이다.

				SiteCTModel sctModel = new SiteCTModel();

				String siteNameOrg = spans.get(1).text().trim();

				sctModel.setSiteNameOrg(siteNameOrg);
				// serch whether or not the site name exists in the reference.
				int srefId = SiteRef.getSrefId(siteNameOrg.replaceAll("\\s", ""));// 뛰어쓰기를 모두 지우고 HashMap에서 검색하여 SrefId
																					// 값을 얻는다.
				if (srefId > 0) {
					sctModel.setSrefId(srefId);
					String siteName = SiteRef.getSiteNameRef(srefId);
					sctModel.setSiteName(siteName);
					ctModel.addSites(siteName);
					if (lead == 1)
						sctModel.setMatchedLeadSite(lead--);
				} else {// no match on referrence
					// sctModel.setSiteName(siteNameOrg);
					if (DEBUG)
						System.out.println("[not in reference]" + siteNameOrg);

					//Log.log(Level.INFO, "[not in reference] " + siteNameOrg);

				}
				sctModel.setSiteOrder(order++);
				sctModel.setAddress(spans.get(3).text().trim());
				sctModel.setCorrectDate(spans.get(5).text().trim());

				ctModel.addSct(sctModel);
			}
//System.out.println("   222   " + ctModel.getSctList().size());

			if (ctModel.getSctList().size() == 0 && !sites.equals("")) {
				order = 1;
				String[] args = sites.split("/");
//System.out.println(" sites= " + sites);
				for (int i = 0; i < args.length; i++) {
			
//System.out.println(i + " order= " + order);
					SiteCTModel sctModel = new SiteCTModel();

					String siteNameOrg = args[i].trim();
//	System.out.println(i + " siteNameORG= " + siteNameOrg);

					if (siteNameOrg.equals(""))
						continue;

					sctModel.setSiteNameOrg(siteNameOrg);
					// serch whether or not the site name exists in the reference.
					int srefId = SiteRef.getSrefId(siteNameOrg.replaceAll("\\s", ""));// 뛰어쓰기를 모두 지우고 HashMap에서 검색하여
//System.out.println("srefId==" + srefId);
					// SrefId 값을 얻는다.
					if (srefId > 0) {
						sctModel.setSrefId(srefId);
						String siteName = SiteRef.getSiteNameRef(srefId);
//System.out.println(srefId +"    "+order+ " siteName= " + siteName);

						sctModel.setSiteName(siteName);
						ctModel.addSites(siteName);
						if (lead == 1)
							sctModel.setMatchedLeadSite(lead--);
					} else {// no match on referrence
						// sctModel.setSiteName(siteNameOrg);
						if (DEBUG)
							System.out.println("[not in reference]" + siteNameOrg);

						//Log.log(Level.INFO, "[not in reference] " + siteNameOrg);

//System.out.println("   111   " + ctModel.getSctList().size() + sctModel.getSiteName());
					}

					sctModel.setSiteOrder(order++);
					ctModel.addSct(sctModel);
				}
//System.out.println("   222   " + ctModel.getSctList().size());
			}
			ConnectDB.writeModel(ctModel);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			//Log.log(Level.SEVERE, "[detail link]" + aref + "\t" + e.toString());
			System.out.println("[detail link]" + aref + "\t" + url + "\t" + e.toString());

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			//Log.log(Level.SEVERE, "[detail link]" + aref + "\t" + url + "\t" + e.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			//Log.log(Level.SEVERE, "[detail link]" + aref + "\t" + url + "\t" + e.toString());
		}
	}

	public static int extractOneNum(String s) {
		ArrayList<Integer> numList = new ArrayList<Integer>();
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(s);
		while (m.find()) {
			numList.add(Integer.parseInt(m.group()));
		}

		if (numList.size() == 1)
			return numList.get(0);
		else
			return -1;

	}

	class ReadCustomException extends Exception {
		public ReadCustomException(String message) {
			super(message);
		}
	}
}
