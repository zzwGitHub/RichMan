package top.ziweb.pojo;

public class Record {
    private Integer id;

    private String operationTime;

    private String openid;

    private String groupid;

    private String nickname;

    private String purposeOpenid;

    private String purposeNickname;

    private String money;

    private String detail;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(String operationTime) {
        this.operationTime = operationTime == null ? null : operationTime.trim();
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid == null ? null : openid.trim();
    }

    public String getGroupid() {
        return groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid == null ? null : groupid.trim();
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname == null ? null : nickname.trim();
    }

    public String getPurposeOpenid() {
        return purposeOpenid;
    }

    public void setPurposeOpenid(String purposeOpenid) {
        this.purposeOpenid = purposeOpenid == null ? null : purposeOpenid.trim();
    }

    public String getPurposeNickname() {
        return purposeNickname;
    }

    public void setPurposeNickname(String purposeNickname) {
        this.purposeNickname = purposeNickname == null ? null : purposeNickname.trim();
    }

    public String getMoney() {
        return money;
    }

    public void setMoney(String money) {
        this.money = money == null ? null : money.trim();
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail == null ? null : detail.trim();
    }
}