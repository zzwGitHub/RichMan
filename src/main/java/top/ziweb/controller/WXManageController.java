package top.ziweb.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.alibaba.fastjson.JSONObject;
import top.ziweb.util.WXOperation;

@Controller
@RequestMapping(value = "/WXManage")
public class WXManageController {

	private static Logger logger  =  Logger.getLogger(WXManageController. class );
	
	/**
	 * 微信用户登录，获取sessionKey和Openid
	 * 需要返回JSESSION
	 * @author Ziw
	 * @date 2018年8月23日
	 */
	@ResponseBody
	@RequestMapping(value = "wxCheckLogin")
	public String wxCheckLogin(String code, HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONObject jobj = WXOperation.checkLogin(code);
		if(jobj.get("openid") != null){
			request.getSession().setAttribute("openid", jobj.get("openid").toString());
			request.getSession().setAttribute("session_key", jobj.get("session_key").toString());
		}
		String sessionid = request.getSession().getId();
		return sessionid;
	}
	
	/**
	 * 解密微信客户端的getShareInfo方法的参数
	 * 然后，把openGId
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
			res = WXOperation.wxDecryption(encryptedData, iv,key);
			request.getSession().setAttribute("openGId", res.get("openGId").toString());
			
		}
		logger.warn("Controller  wxDecryption=====" + res.toJSONString());
		return res;//{"watermark":{"appid":"wxe2e04de9f9bc7eb2","timestamp":1534994509},"openGId":"GsjyK5VQULwWBgNh_5xw-xf7byuc"}
	}
}
