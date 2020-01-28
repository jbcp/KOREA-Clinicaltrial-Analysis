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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* 紐⑹쟻: 二쇰떒�쐞濡� BASEURL�뿉 �엳�뒗 �쎒�궗�씠�듃�뿉 �젒�냽�븯�뿬 �젙蹂대�� 留ㅻ쾲 �깉濡쒖슫 database�뿉 ���옣�븳�떎.
 * 
 *  
 * Required library : Jsoup -1.11.3       
 * 2018-6-25 �씠吏��삎 
 * 
 * 
 * baseurl �씠 蹂�寃쎈맖 2019-1�썡 �솗�씤.
 */
/* 濡쒖쭅 �닚�꽌
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
	// 留곹겕
	public final static boolean DEBUG =false;
	public static String DIR;
	

	public final static Logger LOG = Logger.getLogger("AnalysisCT");

	public AnalysisCT() {
}

	public static void main(String[] args) {
		DIR = System.getProperty("user.dir") + File.separator;
		String logpath = DIR.replace("src", "") + File.separator + "log" + File.separator;
		String configPath = DIR.replace("src", "") + File.separator + "bootstrap"+File.separator+"config" + File.separator;

		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Date myDate = new Date(System.currentTimeMillis());
	



		try {

			FileHandler fh = new FileHandler(logpath + "analysisCT.log",true);
			fh.setFormatter(new SimpleFormatter() {
				public String format(LogRecord log) {

					String logOutput = "[" + (new Date()).toString() + "][" + log.getLevel() + "] MSG[ "
							+ log.getMessage() + "]\n";

					return logOutput;
				}
			});
			fh.setEncoding("utf-8");

			LOG.addHandler(fh);

			
			System.out.println("logpath: " +   logpath);
			System.out.println("configpath:  " +   configPath);
			LocalDateTime currentTime = LocalDateTime.now();
			LOG.log(Level.INFO,"===================Current DateTime: " + currentTime);
			LOG.log(Level.INFO,"logpath: " + logpath);
			LOG.log(Level.INFO,"configpath:  " + configPath);
			
			
			AnalysisCT act = new AnalysisCT();
			SiteRef sref = new SiteRef();
			
				                        String query = sref.getSiteRefDataInsertQuery();
System.out.println("query===== " +query);
LOG.log(Level.INFO, "query===== " +query);	

			// setup DB setting
			ConnectDB.createDb();

			
			// if (DEBUG) sref.debugPrint();
			
			ConnectDB.sendWriteQuery(query);

			act.startWebcrawling();
			
			
			if (ConnectDB.isUpdated()) {// 以묎컙�뿉 �뒪�겕�옒�븨�씠 �쎒�궗�씠�듃 �씫�떎媛� �뿉�윭�굹�뒗 寃쎌슦媛� 醫낆쥌 �깮源�: �븘留덈룄 �쓳�떟�떆媛꾨븣臾몄씤�벏
				// .. �뵲�씪�꽌 留덉�留됱뿉 珥� 媛쒖닔瑜� �뙆�븙�빐�꽌 �떎�떆 �뒪�겕�옒�븨�븯�뜕吏� �뾽�뜲�씠�듃 �븞�븯�뒗寃� 醫뗫떎.
				ConnectDB.writeConfigFile(configPath + "db.ini");

				ConnectDB.exportExcelFile(DIR.replace("src", "") + File.separator + "bootstrap"+File.separator+"public" + File.separator+"excelfiles"+File.separator);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOG.log(Level.SEVERE, e.toString());
		}
	}

	public void startWebcrawling() throws SQLException, Exception {
		Document doc;

		doc = Jsoup.connect(BASEURL).get();
		// if (DEBUG) {
	//	String testURL ="https://nedrug.mfds.go.kr/pbp/CCBBC01/getList?totalPages=4&page=4&limit=10&sort=&sortOrder=&searchYn=&applyEntpName=&approvalDtStart=2010-03-01&approvalDtEnd=2010-03-31&prductName=&clinicStepCode=&clinicExamTitle=";
// doc = Jsoup.connect(testURL).get();
		// }

		Elements boardCountEls = doc.getElementsByClass("board_count");
		Elements spanEls = boardCountEls.select("span");
		int totalListNum = Integer.parseInt(spanEls.get(0).text().trim().replaceAll("[^0-9]+", ""));
		int totalPages = (int) Math.ceil(totalListNum / 10.0);
		// double ceilPages = Math.ceil(pages);
		// System.out.println( Math.ceil(pages));
		if (DEBUG)
			System.out.println("total list num=" + totalListNum + "\ttotalPages=" + totalPages);

		LOG.log(Level.INFO, "total list num=" + totalListNum);
		readAll(totalPages);
	}

	public void readAll(int totalPages) throws SQLException, IOException, Exception {

		for (int i = totalPages; i > 0; i--) {
			String url = BASEURL + "/getList?totalPages=" + totalPages + "&page=" + i + "&limit=10";
			try {
				readAPage(url);// thread濡� �븷�닔�룄 �엳吏�留� 洹몃윺�젮硫� �궇吏쒖닚�씠 �뿁留앹씠 �맆�닔 �엳�쓬.
			} catch (IOException e) {

				readAPage(url);
			}
	//if(DEBUG & (i==totalPages-2)) break;
		}

	}

	public void readAPage(String url) throws IOException, SQLException, Exception {
//		if (DEBUG)
//			System.out.println("[A page link] " + url);

		// �뀒�씠釉붿뿉�꽌 �븳以꾩뵫 泥섎━�빐�빞�븯誘�濡� tr elements 瑜� 李얜뒗�떎. tb_list>tbody>tr
		Document doc = Jsoup.connect(url).get();
		Element tableEl = doc.getElementsByClass("tb_list").first();
		Element tbodyEl = tableEl.getElementsByTag("tbody").first();
		Elements trs = tbodyEl.getElementsByTag("tr");

		for (int i = trs.size() - 1; i >= 0; i--) { // �쎒 �궗�씠�듃媛� �뿭�닚�씠誘�濡� tr�쓣 留덉�留됱뿉�꽌遺��꽣 ���옣�빐�빞�븳�떎.
			Element tr = trs.get(i);

			saveRaw(tr,url);
			

		}

	}

	/*
	 * 紐⑹쟻: tb_list �쓽 �븯�굹�쓽 �뻾�뿉�꽌 �떎�쓬 �뜲�씠�꽣瑜� 異붿텧�븯�뿬 DB�뿉 ���옣 1. "�떊泥��옄" �뿴 link �뵲�씪 �젒�냽�븯�뿬 �븳踰덈뜑 �겕濡ㅻ쭅�븳�떎.
	 * 2. "�떎�떆湲곌�紐�" �뿴�쓽 �뜲�씠�꽣瑜� array 濡� ���옣�븳�떎. 3. 紐⑤뜽�쓣 �씠�슜�븯�뿬 DB�뿉 ���옣�븳�떎.
	 *
	 * sites data �삤瑜� ( tag2 移쇰읆 �뿉 諛섏쁺) 1. �삤湲�: 怨좎궛���븰援먮났�쓬蹂묒썝 ->怨좎떊���븰援먮났�쓬蹂묒썝 2. ���쟾���븰援먮��쟾�븳諛⑸퀝�썝
	 * ->���쟾���븰援� �몦�궛�븳諛⑸퀝�썝 3. 以묒븰���븰援먯쓽怨쇰��븰遺��냽�슜�궛蹂묒썝 ->以묒븰���븰援먮퀝�썝 4. �쓣吏�蹂묒썝 ->�쓣吏����븰援� �쓣吏�蹂묒썝 5. �븳由쇰��븰援�
	 * 媛뺣룞�꽦�떖蹂묒썝 ->�꽦�떖�쓽猷뚯옱�떒 媛뺣룞�꽦�떖蹂묒썝 6. �썝愿묐��븰援먮퀝�썝 ->�썝愿묐��븰援먯쓽怨쇰��븰蹂묒썝 7. 媛뺣룞寃쏀씗���븰援먮퀝�썝->媛뺣룞寃쏀씗���븰援먯쓽��蹂묒썝
	 * 
	 * �뾾�뒗 寃쎌슦 NULL 泥섎━ 1. �쓽猷뚮쾿�씤 �삁�꽦�쓽猷뚯옱�떒 踰좊뜲�뒪�떎蹂묒썝 2. �븰援먮쾿�씤 �씤�븯蹂묒썝
	 */
	public void saveRaw(Element trforaCT,String url) {
		String aref = null;
		try {
			ClinicalTrialModel ctModel = new ClinicalTrialModel();// DataModel
			Elements tdsforaCT = trforaCT.getElementsByTag("td");		
			String no = tdsforaCT.get(0).text().trim();
			ctModel.setNo(Integer.parseInt(no));
			
			Element link = trforaCT.getElementsByTag("a").first(); // "�떊泥��옄" �뿴�쓽 link 媛믪씠�떎.
			
			aref = link.attr("abs:href");
//			if (DEBUG)
//	System.out.println("\t[detail link] " + aref);// detail link
//		
//			String progress = tdsforaCT.get(7).getElementsByTag("span").get(1).text().trim();// 吏꾪뻾�쁽�솴
//			ctModel.setProgress(progress);

			// �쐞�뿉�꽌 援ы븳 "�떊泥��옄"link�뿉 �젒�냽�븯�뿬 �뜲�씠�꽣瑜� �룞�씪 DB�뿉 ���옣�븳�떎.

			Document detailDoc = Jsoup.connect(aref).get();

			Elements detailTbodys = detailDoc.getElementsByTag("tbody");

			Element applicationInfoTbody = detailTbodys.get(0);// �엫�긽�떆�뿕 �떊泥��젙蹂�
			Elements applicationInfoTds = applicationInfoTbody.getElementsByTag("td");// �엫�긽�떆�뿕 �떊泥��젙蹂�

			if (applicationInfoTds.size() != 3) {
				  for( int i=0;i<applicationInfoTds.size();i++) {
						String s=applicationInfoTds.get(i).text().trim();
						System.out.println(i+ "==== " +s + "   lnegth="+s.length());
					}
				  throw new ReadCustomException("ERROR : detail website �엫�긽�떆�뿕 �떊泥��젙蹂�  3!= " + applicationInfoTds.size() + "  on " + aref+ "  on " + url);
			}
			ctModel.setTitle(applicationInfoTds.get(0).text().trim());
			
			
			ctModel.setApplicant(applicationInfoTds.get(1).text().trim());
			

			ctModel.setApprovalDate(applicationInfoTds.get(2).text().trim());

			Element drugInfoTbody = detailTbodys.get(1);// �엫�긽�떆�뿕�쓽�빟�뭹 湲곕낯�젙蹂�
			Elements drugInfoTds = drugInfoTbody.getElementsByTag("td");// �엫�긽�떆�뿕�쓽�빟�뭹 湲곕낯�젙蹂�
			if (drugInfoTds.size() != 4) {
				  for( int i=0;i<drugInfoTds.size();i++) {
						String s=drugInfoTds.get(i).text().trim();
						System.out.println(i+ "==== " +s + "   lnegth="+s.length());

					}
				  throw new ReadCustomException("ERROR : detail website �엫�긽�떆�뿕 �떊泥��젙蹂�  3!= " + applicationInfoTds.size() + "  on " + aref+ "  on " + url);

			}
			ctModel.setProduct(drugInfoTds.get(0).text().trim());
			ctModel.setComponent(drugInfoTds.get(1).text().trim());
			ctModel.setComparator(drugInfoTds.get(2).text().trim());
			ctModel.setPlacebo(drugInfoTds.get(3).text().trim());

			Element ctInfoTbody = detailTbodys.get(2);// �엫�긽�떆�뿕 媛쒖슂
			Elements ctInfoTds = ctInfoTbody.getElementsByTag("td");// �엫�긽�떆�뿕 媛쒖슂
			if (ctInfoTds.size() != 5) {
				  for( int i=0;i<ctInfoTds.size();i++) {
						String s=drugInfoTds.get(i).text().trim();
						System.out.println(i+ "==== " +s + "   lnegth="+s.length());

					}
				  throw new ReadCustomException("ERROR : detail website  �엫�긽�떆�뿕 媛쒖슂 5!= " + applicationInfoTds.size() + "  on " + aref+ "  on " + url);			
			}
			ctModel.setTargetDisease(ctInfoTds.get(0).text().trim());
			ctModel.setPhase(ctInfoTds.get(1).text().trim());
			ctModel.setType(ctInfoTds.get(2).text().trim());
			ctModel.setGender(ctInfoTds.get(3).text().trim());
			String totalsubject = ctInfoTds.get(4).text().trim();
			ctModel.setTotalSubject(totalsubject);
			// extract number to set setNumTotalSubject() and setNumDomesticSubject()
			if (totalsubject != null && !totalsubject.equals("")) { // 鍮덇났��.

				totalsubject = totalsubject.replaceAll(",", "");
				int wholeIndex = totalsubject.indexOf("�쟾泥�");
				int domesticIndex = totalsubject.indexOf("援��궡");
				if (wholeIndex >= 0 && domesticIndex >= 0) { // �쟾泥� 援��궡 �룞�떆

					String whole = totalsubject.substring(0, domesticIndex - 1);
					String domestic = totalsubject.substring(domesticIndex);
					ctModel.setNumTotalSubject(extractOneNum(whole));
					ctModel.setNumDomesticSubject(extractOneNum(domestic));

				} else if (domesticIndex >= 0) { // 援��궡留�

					ctModel.setNumDomesticSubject(extractOneNum(totalsubject));
				} else if (wholeIndex >= 0) { // �쟾泥대쭔

					ctModel.setNumTotalSubject(extractOneNum(totalsubject));
				} else { // unknwon error
					System.out.println("error doing extract num_total_subject and num_domestic_subject. it is not one number " + totalsubject+" on "+  aref +"  on  "+url);

					LOG.log(Level.WARNING, "error doing extract num_total_subject and num_domestic_subject.  it is not one number " + totalsubject+" on "+  aref +"  on  "+url);

				}
			}
			Element siteInfoTbody = detailTbodys.get(3);// �엫�긽�떆�뿕 �떎�떆湲곌� �젙蹂�
			Elements trs = siteInfoTbody.getElementsByTag("tr");
			// system.out.println(trs.size());
			int order=1;
			int lead=1;
			for (int i = 0; i < trs.size(); i++) {
				Element tr = trs.get(i);
				Elements spans = tr.getElementsByTag("span");
				if (spans.size() < 6)// 癒몃━湲�留� �엳�뒗 寃쎌슦 size==1
					continue;  //�븳以� 湲곌� �젙蹂댁쓽 spans.size()==6 �씠�떎. 
//				if (spans.size() == 1 ) {// 癒몃━湲�留� �엳�뒗 寃쎌슦  size==1 �씠誘�濡� �젣�쇅�븷寃�.	
//					Object obj= spans.get(0);
//					if( obj==null)break;
//					if( obj instanceof String) {
//						if(obj.equals(""))	break;		
//					}						
//				}
//				for(int k=0; k<spans.size();k++) {
//					System.out.println("spans.size()="+spans.size()+"   i="+k+"  spans.get(i).text()= "+spans.get(k).text());
//				}
				
				//System.out.println("spans.size()"+spans.size());
				SiteCTModel sctModel = new SiteCTModel();
				
		
				String siteNameOrg = spans.get(1).text().trim();
				
	
				
				sctModel.setSiteNameOrg(siteNameOrg);
				// serch whether or not the site name exists in the reference.
				int srefId = SiteRef.getSrefId(siteNameOrg.replaceAll("\\s", ""));// �쎇�뼱�벐湲곕�� 紐⑤몢 吏��슦怨� HashMap�뿉�꽌 寃��깋�븯�뿬 SrefId 媛믪쓣
																				// �뼸�뒗�떎.
				if (srefId > 0) {
					sctModel.setSrefId(srefId);
					String siteName=SiteRef.getSiteNameRef(srefId);					
					sctModel.setSiteName(siteName);
					ctModel.addSites(siteName);
					if(lead==1) sctModel.setMatchedLeadSite(lead--);
				} else {// no match on referrence
				//	sctModel.setSiteName(siteNameOrg);
					if (DEBUG)
						System.out.println("[not in reference]" + siteNameOrg);

					LOG.log(Level.INFO, "[not in reference] " + siteNameOrg);
					
				}
				sctModel.setSiteOrder(order++);
				sctModel.setAddress(spans.get(3).text().trim());
				sctModel.setCorrectDate(spans.get(5).text().trim());
				
				ctModel.addSct(sctModel);
			}

			// save to mysql
			ConnectDB.writeModel(ctModel);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			LOG.log(Level.SEVERE, "[detail link]" + aref + "\t" + e.toString());
			System.out.println("[detail link]" + aref + "\t" + url+"\t"+ e.toString());

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			LOG.log(Level.SEVERE, "[detail link]" + aref + "\t" + url+"\t"+e.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			LOG.log(Level.SEVERE, "[detail link]" + aref + "\t" + url+"\t"+e.toString());
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

	


	class ReadCustomException extends Exception
	{
	  public ReadCustomException(String message)
	  {
	    super(message);
	  }
	}
}
