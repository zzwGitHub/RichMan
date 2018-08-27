package top.ziweb.controller;

import java.io.IOException;
import java.util.Set;

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
		String sessionid = request.getSession().getId();
		JSONObject jobj = WXOperation.checkLogin(code);
		JSONObject res = new JSONObject();
		res.put("JSESSIONID", sessionid);
		
		if(jobj.get("openid") != null){
			request.getSession().setAttribute("openid", jobj.get("openid").toString());
			request.getSession().setAttribute("session_key", jobj.get("session_key").toString());
			res.put("openid", jobj.get("openid").toString());
		}
		return res.toJSONString();
	}
	
	/**
	 * 微信客户端已然确定，使用者存于组中，且将组信息发来
	 * 我则先解密，然后，把openGId存session，
	 * 还有：放入Redis
	 *
	 * @author Ziw
	 * @date 2018年8月23日
	 */
	@RequestMapping("/wxDecryption")
	@ResponseBody
	public JSONObject wxDecryption(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		String encryptedData = request.getParameter("encryptedData");
		String iv = request.getParameter("iv");
		
		JSONObject res = new JSONObject();
		//必须之前wx.login过，才能进行解密操作
		if(request.getSession().getAttribute("session_key") != null){
			String key = request.getSession().getAttribute("session_key").toString();
			res = WXOperation.wxDecryption(encryptedData, iv, key);
			request.getSession().setAttribute("openGId", res.get("openGId").toString());
			
			String openid = request.getSession().getAttribute("openid").toString();
			jedisClientPool.hset(res.get("openGId").toString(), openid, "1000");
			
			playListPush(request);
		}
//		logger.warn("Controller  wxDecryption=====" + res.toJSONString());
		return res;//{"watermark":{"appid":"wxe2e04de9f9bc7eb2","timestamp":1534994509},"openGId":"GsjyK5VQULwWBgNh_5xw-xf7byuc"}
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
		
		Set<String> playOpenids = jedisClientPool.hkeys(openGId);
		
		JSONArray resArr = new JSONArray();//记录本组所有人的信息
		
		for (String openid : playOpenids) {  
			String moenyOfThis = jedisClientPool.hget(openGId, openid);
			JSONObject one = new JSONObject();
			one.put("openid", openid);
			one.put("money", moenyOfThis);
			resArr.add(one);
		}
		
		logger.warn("OE)@#K" + resArr.toJSONString());
		for (String openid : playOpenids) {  
		      System.out.println(openid);  
		      try {
				WXSocket.sendMessage(openid, resArr.toJSONString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}  
		
		
		
		
		
		
		
	}
}
