package top.ziweb.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codehaus.xfire.util.Base64;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WXOperation {

	private static Logger logger  =  Logger.getLogger(WXOperation. class );
		
	/**
	 * 校验登录
	 * @author Ziw
	 * @date 2018年8月23日
	 */
	public static JSONObject checkLogin(String code){
		String APPID = "wxdac5ad8cb7f0f7e2";
		String SECRET = "ed057d2bf930b747f7c39909cf75ee62";
		String JSCODE = code;
		
		String url = "https://api.weixin.qq.com/sns/jscode2session?appid="+APPID+"&secret="+SECRET+"&js_code="+JSCODE+"&grant_type=authorization_code";
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder()
			.url(url)
			.build();
		
		Response response;
		String res =  "";
		try {
			response = client.newCall(request).execute();
			res =  response.body().string();//{"session_key":"ej6lSvZlG+oInQ35McaZsw==","openid":"osjyK5dz2-_vhenKyHlJ0mgykFPE"}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONObject jobj = JSONObject.parseObject(res);
		
		logger.warn(jobj.toJSONString());
		return jobj;
	}
	
	/**
	 * 解密
	 * @author Ziw
	 * @date 2018年8月23日
	 */
	public static JSONObject wxDecryption(String encryptedData, String iv, String WXSessionKey){
		JSONObject res = getClearText(encryptedData, WXSessionKey, iv);
		//logger.warn(res.toJSONString());
		return res;//{"watermark":{"appid":"wxe2e04de9f9bc7eb2","timestamp":1534994509},"openGId":"GsjyK5VQULwWBgNh_5xw-xf7byuc"}
	
	}
	
	
	/** 
     * 解密用户敏感数据获取用户信息 
     *  
     * @author zhy 
     * @param sessionKey 数据进行加密签名的密钥 
     * @param encryptedData 包括敏感数据在内的完整用户信息的加密数据 
     * @param iv 加密算法的初始向量 
     * @return 
     */  
    private static JSONObject getClearText(String encryptedData,String sessionKey,String iv){  
        // 被加密的数据  
        byte[] dataByte = Base64.decode(encryptedData);  
        // 加密秘钥  
        byte[] keyByte = Base64.decode(sessionKey);  
        // 偏移量  
        byte[] ivByte = Base64.decode(iv);  
        try {  
               // 如果密钥不足16位，那么就补足.  这个if 中的内容很重要  
            int base = 16;  
            if (keyByte.length % base != 0) {  
                int groups = keyByte.length / base + (keyByte.length % base != 0 ? 1 : 0);  
                byte[] temp = new byte[groups * base];  
                Arrays.fill(temp, (byte) 0);  
                System.arraycopy(keyByte, 0, temp, 0, keyByte.length);  
                keyByte = temp;  
            }  
            // 初始化  
            Security.addProvider(new BouncyCastleProvider());  
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding","BC");  
            SecretKeySpec spec = new SecretKeySpec(keyByte, "AES");  
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");  
            parameters.init(new IvParameterSpec(ivByte));  
            cipher.init(Cipher.DECRYPT_MODE, spec, parameters);// 初始化  
            byte[] resultByte = cipher.doFinal(dataByte);  
            if (null != resultByte && resultByte.length > 0) {  
                String result = new String(resultByte, "UTF-8");  
                return JSON.parseObject(result);  
            }  
        } catch (NoSuchAlgorithmException e) {  
            logger.error(e.getMessage(), e);  
        } catch (NoSuchPaddingException e) {  
            logger.error(e.getMessage(), e);  
        } catch (InvalidParameterSpecException e) {  
            logger.error(e.getMessage(), e);  
        } catch (IllegalBlockSizeException e) {  
            logger.error(e.getMessage(), e);  
        } catch (BadPaddingException e) {  
            logger.error(e.getMessage(), e);  
        } catch (UnsupportedEncodingException e) {  
            logger.error(e.getMessage(), e);  
        } catch (InvalidKeyException e) {  
            logger.error(e.getMessage(), e);  
        } catch (InvalidAlgorithmParameterException e) {  
            logger.error(e.getMessage(), e);  
        } catch (NoSuchProviderException e) {  
            logger.error(e.getMessage(), e);  
        }  
        return null;  
    }  
}
