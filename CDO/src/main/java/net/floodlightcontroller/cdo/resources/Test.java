package net.floodlightcontroller.cdo.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.cdo.types.NormalMessage;
import net.floodlightcontroller.cdo.types.UrlType;

public class Test extends ServerResource{

	public Test() {
		// TODO Auto-generated constructor stub
	}
	
	@Post("json")
    public String post(String msg) {
		String resp="failed";  
		//debug 
		System.out.println("[CDO] msg received:"+msg);
      
      
      	NormalMessage req_new_helping_vips=new NormalMessage(NormalMessage.REQ_NEW_HELPING_VIPS,"");
		String t_msg=req_new_helping_vips.ToJsonString();
		
		//for debug
		if(t_msg.isEmpty())
		{
			System.out.println("[debug error] error in parsing json object "+NormalMessage.class);
		}
		
		String msg_ret=SendMessageWithGet("http://127.0.0.1"+":"+UrlType.CXX_API_PORT+UrlType.SUFFIX_CXX_CODE_PAIR_INIT
										  ,t_msg);
		@SuppressWarnings("unused")
		NormalMessage rsp_new_vips=NormalMessage.FromJsonString(msg_ret);
      
      
      return resp;
    }

	protected String SendMessageWithPost(String url, String msg)
	{
		return SendMessage(url,msg,"POST");
	}
	
	protected String SendMessageWithGet(String url, String msg)
	{
		return SendMessage(url,msg,"GET");
	}
	
	private String SendMessage(String url, String msg, String method)
	{		
		System.out.println("[info]trying to send to "+url+" :"+msg);
		
	       HttpURLConnection conn = null; 
	      
	        try {  
	            //   
	            URL mURL = new URL(url);  
	            //  
	            conn = (HttpURLConnection) mURL.openConnection();  
	  
	            conn.setRequestMethod(method); 
	            conn.setRequestProperty("Content-Type","application/json");
	            conn.setDoOutput(true);//    
	   
	            //    
	            OutputStream out = conn.getOutputStream();//    
	            out.write(msg.getBytes());  
	            out.flush();  
	            out.close();  
	  
	            int responseCode = conn.getResponseCode();//   
	            if (responseCode == 200) {  
	  
	                InputStream is = conn.getInputStream();  
	                String state = getStringFromInputStream(is);  
	                return state;  
	            } else {  
//	                
	                System.out.println("failed,responseCode="+responseCode);  
	            }  
	  
	        } catch (Exception e) {  
	            e.printStackTrace();  
	        } finally {  
	            if (conn != null) {  
	                conn.disconnect();//    
	            }  
	        } 
	        return "";
	}
	/** 
     *           * 
     * @param is 
     * @return 
     * @throws IOException 
     */  
    private static String getStringFromInputStream(InputStream is)  
            throws IOException {  
        ByteArrayOutputStream os = new ByteArrayOutputStream();  
        //   
        byte[] buffer = new byte[1024];  
        int len = -1;  
        //   
        // 
        while ((len = is.read(buffer)) != -1) {  
            os.write(buffer, 0, len);  
        }  
        is.close();  
        String state = os.toString();//  
        os.close();  
        return state;  
    }  
}
