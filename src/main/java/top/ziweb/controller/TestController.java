package top.ziweb.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.baidu.aip.speech.AipSpeech;
import com.baidu.aip.speech.TtsResponse;
import com.baidu.aip.util.Util;

import top.ziweb.pojo.Record;
import top.ziweb.redis.JedisClient;
import top.ziweb.service.RecordService;

@Controller
@RequestMapping(value = "/test")
public class TestController {

	@Autowired
	private JedisClient jedisClientPool;
	
	@Autowired
	private RecordService recordService;
	
	@ResponseBody
	@RequestMapping(value = "connect")
	public String connect(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return "connct success!";
	}
	
	@ResponseBody
	@RequestMapping(value = "testSession")
	public String testSession(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		/*HttpSession sesssion = request.getSession();
		System.out.println(sesssion.getId());
		String testmsg = "a";
		Object obj = sesssion.getAttribute("msg");
		if(obj != null){
			testmsg = obj.toString();
			testmsg = testmsg + "a";
		}
		sesssion.setAttribute("msg", testmsg);*/
//		System.out.println("openid-------" + request.getSession().getAttribute("openid"));
		String res = request.getSession().getAttribute("openid").toString();
//		System.out.println(res);
		return res;
	}
	
	@ResponseBody
	@RequestMapping(value = "redis",produces = "application/json; charset=utf-8")  
	public String redis(HttpServletRequest request, HttpServletResponse response) throws Exception {
		jedisClientPool.set("lll", "sdfsf");
		jedisClientPool.hset("ff", "a:32", "sf");
		return "redis success!";
	}
	
	@ResponseBody
	@RequestMapping(value = "ss",produces = "application/json; charset=utf-8")
	public String ss(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		List<Record> a = recordService.list();
		
		String res = JSONObject.toJSONString(a);
		System.out.println(res);
		
		return res;
	}
	
	
	
	
}
