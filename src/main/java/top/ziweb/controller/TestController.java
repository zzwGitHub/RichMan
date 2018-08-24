package top.ziweb.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import top.ziweb.redis.JedisClient;
import top.ziweb.redis.JedisClientPool;

@Controller
@RequestMapping(value = "/test")
public class TestController {

	@Autowired
	private JedisClient jedisClientPool;
	
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
		System.out.println("openid-------" + request.getSession().getAttribute("openid"));
		String res = request.getSession().getAttribute("openid").toString();
		System.out.println(res);
		return res;
	}
	
	@ResponseBody
	@RequestMapping(value = "redis")
	public String redis(HttpServletRequest request, HttpServletResponse response) throws Exception {
		jedisClientPool.set("lll", "sdfsf");
		
		return "redis success!";
	}
}
