
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
/*
 * clinicaltrial table의 각 ct들과 관련된 sites 모델
 * site_ref table 에 있지 않은 경우 sref_id 값은 null 이며
 * ct_no 는 NotNULL 이다.
 * site_name을 site_ref 에서 찾아 있으면 site_name_ref열과 sref_id 열에 저장한다. 
 * 
 * */
public class SiteCTModel {

	///////////////////////////////////////////////////////////////////////////////////////
	private int sctId;//인공 pk //자동, not null
	private int ctId; //fk from clinicaltrial table , not null
	private Integer sref_id; // fk from site_ref // reference에 없는 경우 null
	private int siteOrder=0; //나오는 실시기간 순으로 1부터 카운팅.
	private int matchedLeadSite=0; //if  승인기관리스트 sref 와 매치하는  맨 처음 실시기관 1 나머지는 default 0;

	private String siteName; //site_ref table 에 매칭되면 if(sref_id) siteNameRef: siteNameOrg 
	private String siteNameOrg;// 실시기관이름 web 에 있는 그대로
	private String address;// 주소
	private String location;// 지역
	private Date correctDate;// 수정일자
	
	public final static int LEN_ADDRESS=25;
	public final static int LEN_SITE_NAME_ORG=50;
	public final static int LEN_SITE_NAME=50;
	public SiteCTModel() {		
		this.ctId=-1;
		this.sref_id=null;
		this.address=null;
		this.location=null;
		this.correctDate=null;
		this.siteName=null;
		this.siteNameOrg=null;
	
	}

	@Override
	public String toString() {
		return " sctId=" + sctId +"ctId=" + ctId + "  sref_id=" + sref_id +  "  , siteName=" + siteName
				+ ", siteOrder="+siteOrder+"  matchedLeadSite="+  matchedLeadSite+ "  siteNameOrg= "+siteNameOrg+", address=" + address + ", location=" + location + ", correctDate=" + correctDate + " ]";
						
		
	}

	
	public int getCTId() {
		return ctId;
	}

	public void setCTId(int ctId) {
		this.ctId = ctId;
	}

	public Integer getSrefId() {
		return sref_id;
	}

	public void setSrefId(Integer srefId) {
		this.sref_id = srefId;
	}

	public int getSiteOrder() {		
		return siteOrder;
	}

	public void setSiteOrder(int siteOrder) {
		this.siteOrder = siteOrder;
	}

	public int getMatchedLeadSite() {		
		return matchedLeadSite;
	}

	public void setMatchedLeadSite(int matchedLeadSite) {
		this.matchedLeadSite = matchedLeadSite;
	}


	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = Util.validate(siteName, LEN_SITE_NAME);
	}

	public String getSiteNameOrg() {
		return siteNameOrg;
	}

	public void setSiteNameOrg(String siteNameOrg) {
		this.siteNameOrg = Util.validate(siteNameOrg, LEN_SITE_NAME_ORG);
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = Util.validate(address, LEN_ADDRESS);
		setLocation(address.substring(0,4));
		//System.out.println(this.location);
	}

	public String getLocation() {
		return location;
	}
/*
 * 행정구역 2019.2.
 *  서울특별시
 *  부산광역시
 *  대구광역시
 *  인천광역시
 *  광주광역시
 *  대전광역시
 *  울산광역시
 *  세종특별자치시
 *  경기도
 *  강원도
 *  충청북도
 *  충청남도
 *  전라북도
 *  전라남도
 *  경상북도
 *  경상남도
 *  제주특별자치도
 */
	public void setLocation(String location) {
		if(location.startsWith("충청북도")) this.location="충북";
		else if (location.startsWith("충청남도")) this.location="충남";
		else if (location.startsWith("전라북도")) this.location="전북";		
		else if (location.startsWith("전라남도")) this.location="전남";	
		else if (location.startsWith("경상북도")) this.location="경북";	
		else if (location.startsWith("경상남도")) this.location="경남";	
		else  this.location=location.substring(0,2);			
	}

	public Date getCorrectDate() {
		return correctDate;
	}

	public void setCorrectDate(String  correctDate) throws Exception {
		this.correctDate=Util.convertToSQLDate(correctDate);
		 
	}	

	
}


	