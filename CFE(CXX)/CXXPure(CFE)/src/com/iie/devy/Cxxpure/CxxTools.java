package com.iie.devy.Cxxpure;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;

public class CxxTools  {	
	private static Logger logger=Logger.getLogger(CxxTools.class);
	public static String SendMessage(String url, String msg)
	{
		//默认使用POST
		logger.info("Sending to "+url+ " with Message: "+msg);
		return SendMessageWithoutLog(url,msg,"POST");
	}

	public static String SendMessageWithoutLog(String url, String msg, String type)
	{		
	       HttpURLConnection conn = null;  
	        try {  
	            // 创建一个URL对象  
	            URL mURL = new URL(url);  
	            // 调用URL的openConnection()方法,获取HttpURLConnection对象  
	            conn = (HttpURLConnection) mURL.openConnection();  
	  
	            conn.setRequestMethod(type);// type=GET/POST  
	            conn.setRequestProperty("Content-Type","application/json");
	            conn.setDoOutput(true);// 设置此方法,允许向服务器输出内容  
	   
	            // 获得一个输出流,向服务器写数据,默认情况下,系统不允许向服务器输出内容  
	            OutputStream out = conn.getOutputStream();// 获得一个输出流,向服务器写数据   
	            out.write(msg.getBytes());  
	            out.flush();  
	            out.close();  
	  
	            int responseCode = conn.getResponseCode();// 调用此方法就不必再使用conn.connect()方法  
	            if (responseCode == 200) {  
	  
	                InputStream is = conn.getInputStream();  
	                String state = getStringFromInputStream(is);  
	                return state;  
	            } else {  
//	                Log.i(TAG, "访问失败" + responseCode);  
	                System.out.println("failed, responseCode="+responseCode);  
	            }  
	  
	        } catch (Exception e) {  
	            e.printStackTrace();  
	        } finally {  
	            if (conn != null) {  
	                conn.disconnect();// 关闭连接  
	            }  
	        } 
	        return "";
	}
	
	
	
	
	
	/** 
     * 根据流返回一个字符串信息         * 
     * @param is 
     * @return 
     * @throws IOException 
     */  
    private static String getStringFromInputStream(InputStream is)  
            throws IOException {  
        ByteArrayOutputStream os = new ByteArrayOutputStream();  
        // 模板代码 必须熟练  
        byte[] buffer = new byte[1024];  
        int len = -1;  
        // 一定要写len=is.read(buffer)  
        // 如果while((is.read(buffer))!=-1)则无法将数据写入buffer中  
        while ((len = is.read(buffer)) != -1) {  
            os.write(buffer, 0, len);  
        }  
        is.close();  
        String state = os.toString();// 把流中的数据转换成字符串,采用的编码是utf-8(模拟器默认编码)  
        os.close();  
        return state;  
    }  
	
//	//与CDO通信的线程,上报监听信息
//	public static class TdCommCdoUp extends Thread
//	{
//		//用于和监听线程通信的BlockingQueue
//		private BlockingQueue<String> m_bqCommFromStatListen;
//		private CXX m_cxx;
//		
//		
//		private boolean m_flag_shutdown=false;
//		
//		public TdCommCdoUp(BlockingQueue<String> bq, CXX cxx)
//		{
//			this.m_bqCommFromStatListen=bq;
//			this.m_cxx=cxx;	
//		}
//		@Override
//		public void run() {
//			System.out.println("Thread TdCommCdoUp launches");
//			//initiate
//			String message="";
//			
//			//executes
//			while(!m_flag_shutdown)
//			{
//				try {
//					message=this.m_bqCommFromStatListen.take();
//					System.out.println("Sending msg,msg="+message);
//					if(message.contains(""))
//					{
//						
//					}
//					else if(message.contains(""))
//					{
//						
//					}
//					
//					String ret=SendMessage(this.m_url_cdoApi,message);
//					//debug
//					System.out.println("Sending msg, ret="+ret);
//				} catch (InterruptedException e) {
//					System.out.println("Thread TdCommCdoUp errors");
//					e.printStackTrace();
//				}
//			}
//			
//			System.out.println("Thread TdCommCdoUp exits");			
//		}
//		
//		
//		
//		
//	
//		
//		public void Shutdown()
//		{
//			this.m_flag_shutdown=true;
//		}
//		
//		
//		
//	}
//	
	
}
