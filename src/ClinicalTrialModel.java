
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ClinicalTrialModel {

	///////////////////////////////////////////////////////////////////////////////////////

	//private int ctId;
	private Integer no; // 순번
	// ivate String sites;
	private String applicant;// 신청인
	// private String link;
	private Date approvalDate;// 승인일자
	private String product;// 제품명
	private String title;// 시험제목
	private String phase;// 시험단계
	private String progress; // 진행현황
	private String component;// 시험약성분명
	private String comparator;// 대조약(있음/없음)
	private String placebo; // 위약(있음/없음)
	private String targetDisease;// 대상질환명
	private String gender;// 성별
	// private Integer leadSite; // 실시기관 대표
	private String totalSubject;// 전체시험대상자수
	private Integer numTotalSubject;// 전체시험대상자수

	private Integer numDomesticSubject;// 국내시험대상자수
	private String type; // 다국가/국내

	private ArrayList<SiteCTModel> sctList;// 승인된 실시기관 목록에서 찾아 그 원 이름으로 바꾼 실시기관 목록. 이 목록은
	
	private StringBuilder sites; 
	// private ArrayList<Object> amlist;
	//private String cmt;
	public final static int LEN_APPLICANT=25;
	public final static int LEN_PRODUCT=250;
	public final static int LEN_TITLE=1000;
	public final static int LEN_PHASE=10;
	public final static int LEN_COMPONENT=250;
	public final static int LEN_COMPARATOR=2;
	public final static int LEN_PLACEBO=2;
	public final static int LEN_TARGET_DISEASE=500;
	public final static int LEN_GENDER=2;
	public final static int LEN_TOTAL_SUBJECT=30;
	public final static int LEN_TYPE=4;
	public final static int LEN_SITES=1000;
	
	
	public ClinicalTrialModel() {

		this.no = null;
		this.applicant = null;
		this.approvalDate = null;
		this.product = null;
		this.title = null;
		this.phase = null;
		this.progress = null;
		this.component = null;
		this.comparator = null;
		this.placebo = null;
		this.targetDisease = null;
		this.gender = null;
		this.totalSubject = null;
		this.numTotalSubject = null;
		this.numDomesticSubject = null;
		this.type = null;
		//this.cmt = null;
		sctList = new ArrayList<SiteCTModel>();
		this.sites=new StringBuilder();
	}

	public String getSites() {
		if(sites.length()==0) return null;
		return sites.toString();
	}

	public void addSites(String asite) {
		if(sites.length()==0) this.sites.append(asite);
		else this.sites.append(","+asite);
	}

	public ArrayList<SiteCTModel> getSctList() {
		return sctList;
	}


	public void setSctList(ArrayList<SiteCTModel> sctArr) {
		this.sctList = sctArr;
	}

	public void addSct(SiteCTModel sct) {
		this.sctList.add(sct);
	}

	@Override
	public String toString() {
		return "no=" + no + "  applicant=" + applicant + ", approveDate=" + approvalDate + "  , progress=" + progress
				+ ", product=" + product + ", title=" + title + ", phase=" + phase + ", component=" + component
				+ ", comparator=" + comparator + ", placebo=" + placebo + ", targetDisease=" + targetDisease
				+ ", gender=" + gender + ",  totalSubject=" + totalSubject + ",  numTotalSubject=" + numTotalSubject
				+ ", numDomesticSubject=" + numDomesticSubject + ",   type=" + type + "]";
	}


	public Integer getNo() {
		return no;
	}

	public void setNo(Integer no) {
		this.no = no;

	}

//	public int getCTId() {
//		return ctId;
//	}
//
//	public void setCTId(int ct_id) {
//		this.ctId = ct_id;
//
//	}

	public void setApprovalDate(Date approvalDate) {
		this.approvalDate = approvalDate;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getApplicant() {
		return applicant;
	}

	public void setApplicant(String applicant) {
		
		this.applicant = Util.validate(applicant,LEN_APPLICANT);
	}

	public Date getApprovalDate() {

		return approvalDate;
	}

	public void setApprovalDate(String approvalDate) throws Exception {
		this.approvalDate = Util.convertToSQLDate(approvalDate);
	}

	public String getProduct() {

		return product;
	}

	public void setProduct(String product) {
		
		this.product = Util.validate(product,LEN_PRODUCT);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = Util.validate(title,LEN_TITLE);
	}

	public String getPhase() {
		return phase;
	}

	public void setPhase(String phase) {
		this.phase = Util.validate(phase,LEN_PHASE);
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = Util.validate(component,LEN_COMPONENT);
	}

	public String getComparator() {
		return comparator;
	}

	public void setComparator(String comparator) {
		this.comparator = Util.validate(comparator,LEN_COMPARATOR);
	}

	public String getPlacebo() {
		return placebo;
	}

	public void setPlacebo(String placebo) {
		this.placebo = Util.validate(placebo,LEN_PLACEBO);
	}

	public String getTargetDisease() {
		return targetDisease;
	}

	public void setTargetDisease(String targetDisease) {

			this.targetDisease = Util.validate(targetDisease,LEN_TARGET_DISEASE);
	}

	public Integer getNumTotalSubject() {
		return numTotalSubject;
	}

	public void setNumTotalSubject(int numTotalSubject) {
		if(numTotalSubject!=-1 )
			this.numTotalSubject = numTotalSubject;
	}

	public Integer getNumDomesticSubject() {
		return numDomesticSubject;
	}

	public void setNumDomesticSubject(int numDomesticSubject) {
		if(numDomesticSubject!=-1 )
			this.numDomesticSubject = numDomesticSubject;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender =  Util.validate(gender,LEN_GENDER);;
	}

	public String getTotalSubject() {

		return totalSubject;
	}

	public void setTotalSubject(String totalSubject) {
		this.totalSubject = Util.validate(totalSubject,LEN_TOTAL_SUBJECT);
	
	
	}

	public String getProgress() {
		return progress;
	}

	public void setProgress(String progress) {
		this.progress = progress;
	}
	

	
}
