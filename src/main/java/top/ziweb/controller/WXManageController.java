package top.ziweb.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baidu.aip.speech.AipSpeech;
import com.baidu.aip.speech.TtsResponse;
import com.baidu.aip.util.Util;

import top.ziweb.redis.JedisClient;
import top.ziweb.util.WXOperation;
import top.ziweb.websocket.WXSocket;

@Controller
@RequestMapping(value = "/WXManage")
public class WXManageController {

	private static Logger logger  =  Logger.getLogger(WXManageController. class );
	
	@Autowired
	private JedisClient jedisClientPool;
	/**
	 * 微信用户登录，获取sessionKey和Openid
	 * 需要返回JSESSION
	 * @author Ziw
	 * @date 2018年8月23日
	 */
	@ResponseBody
	@RequestMapping(value = "wxCheckLogin")
	public String wxCheckLogin(String code, HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.warn("wxCheckLogin进入");
		String sessionid = request.getSession().getId();
		JSONObject jobj = WXOperation.checkLogin(code);
		JSONObject res = new JSONObject();
		res.put("JSESSIONID", sessionid);
		
		if(jobj.get("openid") != null){
			request.getSession().setAttribute("openid", jobj.get("openid").toString());
			request.getSession().setAttribute("session_key", jobj.get("session_key").toString());
			res.put("openid", jobj.get("openid").toString());

			logger.warn("wxCheckLogin解析并存入session的session_key=====" +jobj.get("session_key").toString() 
					+ "00000000000session_key=======" + jobj.get("openid").toString());
		}
		return res.toJSONString();
	}
	
	/**
	 * 微信端发来了用户基本信息（网名~头像~so on）
	 *
	 * @author Ziw
	 * @date 2018年8月29日
	 */
	@ResponseBody
	@RequestMapping(value = "recordWXUserInfo")
	public JSONObject recordWXUserInfo(String code, HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.warn("recordWXUserInfo进入");
		
		String nickName = request.getParameter("nickName");
		String avatarUrl = request.getParameter("avatarUrl");
		JSONObject res = new JSONObject();
		
		request.getSession().setAttribute("nickName", nickName);
		request.getSession().setAttribute("avatarUrl", avatarUrl);
		
		logger.warn("recordWXUserInfo 中 的nickName=====" + nickName 
				+ "00000000000avatarUrl=======" + avatarUrl);
		
		return res;

	
	}
	
	/**
	 * 微信客户端已然确定"使用者存于组中"，且将组信息发来
	 * 这里先解密，然后，把openGId存session，
	 * 还有：放入Redis
	 *
	 * @author Ziw
	 * @date 2018年8月23日
	 */
	@RequestMapping("/recordWXGroupInfo")
	@ResponseBody
	public JSONObject recordWXGroupInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.warn("recordWXGroupInfo进入" );
		
		String encryptedData = request.getParameter("encryptedData");
		String iv = request.getParameter("iv");
		
		JSONObject res = new JSONObject();
		//必须之前wx.login过，才能进行解密操作
		if(request.getSession().getAttribute("session_key") != null){
			String key = request.getSession().getAttribute("session_key").toString();
			res = WXOperation.wxDecryption(encryptedData, iv, key);
			request.getSession().setAttribute("openGId", res.get("openGId").toString());
			logger.warn("recordWXGroupInfo::::::" + res.toJSONString());
			
			String openid = request.getSession().getAttribute("openid").toString();
			String nickName = request.getSession().getAttribute("nickName").toString();
			String avatarUrl = request.getSession().getAttribute("avatarUrl").toString();
			
			logger.warn("recordWXGroupInfo:::nickName:::" + nickName);
			
			String isExist = jedisClientPool.hget(res.get("openGId").toString(), openid+":money");
			String openGid = res.get("openGId").toString();
			if(isExist == null){//证明是新玩家
				jedisClientPool.hset(openGid, openid + ":money", "10000");
				jedisClientPool.hset(openGid, openid + ":nickName", nickName);
				jedisClientPool.hset(openGid, openid + ":avatarUrl", avatarUrl);
				logger.warn("recordWXGroupInfo:::jedisClientPool:::" + avatarUrl);
				upGroupPlaysList(openGid,openid);
			}else{
				//嘛也不干
			}
			
			playListPush(request);
			pushPlayersMoney(request);
		
			String bankOpenid = jedisClientPool.hget(openGid, "bankOpenid");
		    if(bankOpenid.equals(openid)){ //说明此人是命定之人
		    	res.put("isBank", true);
		    }else{
		    	res.put("isBank", false);
		    }
		}
		return res;//{"watermark":{"appid":"wxe2e04de9f9bc7eb2","timestamp":1534994509},"openGId":"GsjyK5VQULwWBgNh_5xw-xf7byuc"}
	}
	
	/**
	 * Ridis中，每个微信群，都对应着一条Hash数据，
	 * 那条Hash类型的信息（key为组ID）中，存着这一条用于记录全员的列表，
	 * 这则信息就暂且命名为：players吧~~~
	 *
	 * @author Ziw
	 * @date 2018年8月29日
	 */
	private void upGroupPlaysList(String openGid, String openId){
		String palysListStr = jedisClientPool.hget(openGid, "players");
		logger.warn("upGroupPlaysList===upGroupPlaysList::::::" + palysListStr);
		Set<String> palysArray;
		if(palysListStr == null){    //如果reids中没有这个列表就新建
			
			palysArray = new HashSet<String>();
			palysArray.add(openId);
			
			//而且这说明此事是第一位玩家登陆进去
			//就让这个人兼理“bank”吧！
			jedisClientPool.hset(openGid, "bankOpenid", openId);
			
			jedisClientPool.expire(openGid, 1 * 60 * 60 * 24); //每次游戏数据在1天内删除
			
		}else{  //有的话，就向其中加入新数据
			
			palysArray = new HashSet<String>(Arrays.asList(palysListStr.split(","))) ;
			palysArray.add(openId);
		}
		palysListStr = palysArray.toString();
		palysListStr = palysListStr.substring(1, palysListStr.length()-1);
		
		palysListStr = palysListStr.replace(" ","");//鬼知道为毛 set在转字符串后会自动加空格
		
		jedisClientPool.hset(openGid, "players", palysListStr);
	}
	
	
	/**
	 * 通知组内所有人，有人加入啦，把最新的组员名单推送给所有的玩家！！！！
	 *
	 * @author Ziw
	 * @date 2018年8月24日
	 */
	private void playListPush(HttpServletRequest request){
		HttpSession session = request.getSession();
		
		String openGId = session.getAttribute("openGId").toString();
		String palysListStr = jedisClientPool.hget(openGId, "players");
        String[] playOpenids = palysListStr.split(",");
        
		JSONArray resArr = new JSONArray();//记录本组所有人的信息
		for (String openid : playOpenids) {  
			String nickName = jedisClientPool.hget(openGId, openid + ":nickName");
			String money = jedisClientPool.hget(openGId, openid + ":money");
			String avatarUrl = jedisClientPool.hget(openGId, openid + ":avatarUrl");
			JSONObject one = new JSONObject();
			one.put("openid", openid);
			one.put("money", money);
			one.put("name", nickName);
			one.put("pic", avatarUrl);
			resArr.add(one);
		}
		
//		logger.warn("OE)@#K" + resArr.toJSONString());
		for (String openid : playOpenids) {  
			
			JSONObject res = new JSONObject();
			res.put("fun", "showAllPlayers");
			res.put("data", resArr.toJSONString());
			
			try {
				WXSocket.sendMessage(openid, res.toJSONString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}  
	}
	
	/**
	 * 向每一位玩家推送信息，告诉他们自己都有多少钱
	 *
	 * @author Ziw
	 * @date 2018年8月30日
	 */
	private void pushPlayersMoney(HttpServletRequest request){
		HttpSession session = request.getSession();
		
		String openGId = session.getAttribute("openGId").toString();
		String palysListStr = jedisClientPool.hget(openGId, "players");
        String[] playOpenids = palysListStr.split(",");
        
		for (String openid : playOpenids) {  
			
			String myMoney = jedisClientPool.hget(openGId, openid + ":money");
			
			JSONObject res = new JSONObject();
			res.put("fun", "showMyMoney");
			res.put("data", myMoney);
			
			try {
				WXSocket.sendMessage(openid, res.toJSONString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}  
	}
	
	
	/**
	 * 银行页面的金额变动
	 *
	 * @author Ziw
	 * @date 2018年8月28日
	 */
	@RequestMapping("/bankSetMoney")
	@ResponseBody
	public void bankSetMoney(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String money = (request.getParameter("money")==null?"":request.getParameter("money")).toString();
		String human = (request.getParameter("human")==null?"":request.getParameter("human")).toString();
	
		if("".equals(money))  //数据不对别墨迹，直接结束掉
			return;
		
		String openGId = request.getSession().getAttribute("openGId").toString();
		if("".equals(human)){
			//给所有人金额变动
			String palysListStr = jedisClientPool.hget(openGId, "players");
	        String[] playOpenids = palysListStr.split(",");
			for (String openid : playOpenids) {  
				jedisClientPool.hset(openGId, openid + ":money", money);
			}
		}else{
			//给指定人
			jedisClientPool.hset(openGId, human + ":money", money);
		}
		playListPush(request); //最后要推送一下哦~
		pushPlayersMoney(request);
	}
	
	/**
	 * 玩家页面的金额变动
	 * 如果玩家是选择向银行赚钱，则传来的值是bank
	 * 
	 * @author Ziw
	 * @date 2018年8月28日
	 */
	@RequestMapping("/playerSetMoney")
	@ResponseBody
	public void playerSetMoney(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String money = (request.getParameter("money")==null?"":request.getParameter("money")).toString();
		String getMoneyPeople = (request.getParameter("human")==null?"":request.getParameter("human")).toString();
		
		if("".equals(money) || "".equals(getMoneyPeople))  //数据不对别墨迹，直接结束掉
			return;
		
		String openGId = request.getSession().getAttribute("openGId").toString();
		String sendMoneyPeople = request.getSession().getAttribute("openid").toString();
		
		int moneyInt = Integer.parseInt(money);
		
		String myMoney = jedisClientPool.hget(openGId, sendMoneyPeople + ":money");
		int myMoneyInt = Integer.parseInt(myMoney);
		jedisClientPool.hset(openGId, sendMoneyPeople + ":money", (myMoneyInt-moneyInt) + "");
 
		//不是向银行打钱时的操作
		if(!"bank".equals(getMoneyPeople)){ 
			String herMoney = jedisClientPool.hget(openGId, getMoneyPeople + ":money");
			int herMoneyInt = Integer.parseInt(herMoney);
			jedisClientPool.hset(openGId, getMoneyPeople + ":money", (herMoneyInt+moneyInt) + "");
			
		}
		
		playListPush(request); //最后要推送一下哦~
		pushPlayersMoney(request);

		
	}
	
	
	
	public static final String APP_ID = "11797620";
    public static final String API_KEY = "i0zNOugGq1Od3fR1a5tSEoY1";
    public static final String SECRET_KEY = "Pk4iFP3bVk4UQueiYgGfCg313wcalzBO";

	@ResponseBody
	@RequestMapping(value = "CollectMoneyVoice",produces = "application/json; charset=utf-8")  
	public String CollectMoneyVoice(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String receiveMoney = request.getParameter("receiveMoney");
		if(receiveMoney == null || "".equals(receiveMoney)){
			return "";
		}
		
		// 初始化一个AipSpeech
        AipSpeech client = new AipSpeech(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        //client.setSocketTimeoutInMillis(60000);

        // 可选：设置代理服务器地址, http和socket二选一，或者均不设置
        //client.setHttpProxy("proxy_host", proxy_port);  // 设置http代理
        //client.setSocketProxy("proxy_host", proxy_port);  // 设置socket代理

        // 可选：设置log4j日志输出格式，若不设置，则使用默认配置
        // 也可以直接通过jvm启动参数设置此环境变量
        //System.setProperty("aip.log4j.conf", "path/to/your/log4j.properties");

        // 调用接口
        TtsResponse res = client.synthesis("收款" + receiveMoney + "元。", "zh", 1, null);
        byte[] data = res.getData();
        org.json.JSONObject res1 = res.getResult();
        
        String path2 = request.getSession().getServletContext().getRealPath("/");
        System.out.println(path2);
        String voiceName = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        if (data != null) {
            try {
                Util.writeBytesToFileSystem(data, path2 + "voice/"+voiceName+".mp3");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (res1 != null) {
            System.out.println(res1.toString(2));
        }
        
		return voiceName;
	}
	
	
	@ResponseBody
	@RequestMapping(value = "deleteVoice",produces = "application/json; charset=utf-8")  
	public void deleteVoice(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String Mp3Name = request.getParameter("Mp3Name");
		if(Mp3Name == null || "".equals(Mp3Name)){
			return;
		}
		
		String path = request.getSession().getServletContext().getRealPath("/");
        System.out.println(path);
		
        path = path + "voice/"+Mp3Name+".mp3";
        
        
        File file = new File(path);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                System.out.println("删除单个文件" + path + "成功！");
            } else {
                System.out.println("删除单个文件" + path + "失败！");
            }
        } else {
            System.out.println("删除单个文件失败：" + path + "不存在！");
        }
	
	}
	
}
