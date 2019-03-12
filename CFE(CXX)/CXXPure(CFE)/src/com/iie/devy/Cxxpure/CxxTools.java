/*
 * Static tools used by CXX(CFE)
 */

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
		//default : POST
		logger.info("Sending to "+url+ " with Message: "+msg);
		return SendMessageWithoutLog(url,msg,"POST");
	}

	/*The function to send message to a certain URL*/
	public static String SendMessageWithoutLog(String url, String msg, String type)
	{		
	       HttpURLConnection conn = null;  
	        try {  
	            URL mURL = new URL(url);  
	            // Obtain HttpURLConnection object  
	            conn = (HttpURLConnection) mURL.openConnection();  
	  
	            conn.setRequestMethod(type);// type=GET/POST  
	            conn.setRequestProperty("Content-Type","application/json");
	            conn.setDoOutput(true);// allows to output to server  
	   
	            // Obtain an output stream and write to target server 
	            OutputStream out = conn.getOutputStream();  
	            out.write(msg.getBytes());  
	            out.flush();  
	            out.close();  
	            
	            // Get and check response code
	            int responseCode = conn.getResponseCode(); 
	            if (responseCode == 200) {  
	  
	                InputStream is = conn.getInputStream();  
	                String state = getStringFromInputStream(is);  
	                return state;  
	            } else {  
//	                Log.i(TAG, "failed" + responseCode);  
	                System.out.println("failed, responseCode="+responseCode);  
	            }  
	  
	        } catch (Exception e) {  
	            e.printStackTrace();  
	        } finally {  
	            if (conn != null) {  
	                conn.disconnect();// close the connection  
	            }  
	        } 
	        return "";
	}
	
	
	
	
	
	/** 
     * Get string from input stream         * 
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
        // R:len=is.read(buffer)  
        // W:while((is.read(buffer))!=-1)  
        while ((len = is.read(buffer)) != -1) {  
            os.write(buffer, 0, len);  
        }  
        is.close();  
        String state = os.toString();// default with UTF-8  
        os.close();  
        return state;  
    }  
	
//	//Communicate to CDO and upload monitoring information
//	public static class TdCommCdoUp extends Thread
//	{
//		
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
