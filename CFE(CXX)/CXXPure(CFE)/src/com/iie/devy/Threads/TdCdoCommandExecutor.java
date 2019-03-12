/*
 * The thread to execute the command from CDO
 */
package com.iie.devy.Threads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;

import com.iie.devy.Cxxpure.CXX;
import com.iie.devy.Cxxpure.CxxTools;
import com.iie.devy.Cxxpure.Default.UrlType;
import com.iie.devy.Cxxpure.Types.msgtype.NormalMessage;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class TdCdoCommandExecutor extends Thread
{
	private static final Logger logger=Logger.getLogger(TdCdoCommandExecutor.class);

	private int m_port;
	private CXX m_cxx;
	private boolean flag_stop=false;
	
	private boolean flag_debug=false;
	
	public TdCdoCommandExecutor(CXX cxx, int port)
	{
		m_cxx=cxx;
		m_port=port;
	}
	
	public void Shutdown()
	{
		flag_stop=true;
	}
	
	//The handler to deal with new CODE establishment process
	public class HNewCodePair implements HttpHandler
	{
		@Override
		public void handle(HttpExchange arg0) throws IOException {			
		//get configuration information
			 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(arg0.getRequestBody()));
			 String msgStr="";
			 String line="";
			 while( (line=bufferedReader.readLine()) != null)
			 {
				 msgStr += line;
			 }
			 
			 logger.info("[msg received]"+msgStr);
			 
			 NormalMessage msg = NormalMessage.FromJsonString(msgStr);
			 if(null==msg)
			 {
				 logger.error("unable to parse message:"+msgStr);
				 ReturnError(arg0);
				 return;
			 }
			 
			 //Judgment
			 if(msg.GetMsgType().equals(NormalMessage.REQ_NEW_HELPING_VIPS))
			 {
				String vipsName=m_cxx.StartHelpingWorkingVIPS(); 
				int port_ovs_vips=m_cxx.GetOvsVipsPortInfo(vipsName);
				//RespNewVips resp=new RespNewVips(port_ovs_vips);
				NormalMessage resp= new NormalMessage(NormalMessage.RSP_NEW_HELPING_VIPS,vipsName,""+port_ovs_vips);
				ReturnInfo(arg0, resp.ToJsonString());
			 }
			 else if(msg.GetMsgType().equals(NormalMessage.REQ_AVAIL_UFD_VIPS_PORT))
			 { 
			   	 int port_ovs_ufd=m_cxx.GetAvailOvsUfdPort();
			   	 NormalMessage tmsg=new NormalMessage(NormalMessage.RSP_AVAIL_UFD_VIPS_PORT,""+port_ovs_ufd);
			   	 ReturnInfo(arg0, tmsg.ToJsonString());
			 } 
			 else if(msg.GetMsgType().equals(NormalMessage.REQ_OVS_COMMAND))
			 {
				 int tunid=Integer.parseInt(msg.GetParam1());
				 int port=Integer.parseInt(msg.GetParam2());
				 // execute ovs command
				 int ret=m_cxx.InitCodeOvsCommand(tunid,port);
				 if(ret==0)
				 {
					 //record the port information of the helping vips
					 m_cxx.RecordForeignHelpingVipsPortInfo(port);
					 ReturnSuccess(arg0);
				 }
				 else
				 {
					 ReturnError(arg0);
				 }
			 }
			 // deal with the UFD-offload request from CDO to requester
			 else if(msg.GetMsgType().equals(NormalMessage.REQ_UFD_OFFLOAD))
			 {
				 String ip_provider_cfe=msg.GetParam1();
				 String name_new_vips=msg.GetParam2();
				//inform ufd to distribute the flows (see VOCC case 1)
				 String overloaded_vips=m_cxx.GetMaxLoadedVips();
				 String sips=m_cxx.InformUfdToOffload(overloaded_vips);
				 // get the context information related to the SIPs to be offloaded
				 String context=m_cxx.GetContextInfoFromVIPS(overloaded_vips, sips);
				 
				 //inform the provider with the message (vipsname, context related to the sips to be offloaded)
				 NormalMessage msg_context_transfer=new NormalMessage(NormalMessage.REQ_CONTEXT_TRANSFER_CFE,name_new_vips,context);
				 String ret=CxxTools.SendMessage("http://"+ip_provider_cfe+":"+UrlType.CXX_API_PORT+UrlType.SUFFIX_CXX_CODE_PAIR_INIT, msg_context_transfer.ToJsonString());
				 if(ret==UrlType.RESP_SUCCESS)
				 {
					 ReturnSuccess(arg0); 
				 }
				 else
				 {
					 ReturnError(arg0);
				 }
				 
			 }
			 // deal with context transfer request from requester to provider (VOCC case 1)
			 else if(msg.GetMsgType().equals(NormalMessage.REQ_CONTEXT_TRANSFER_CFE))
			 {	 
			//parse message (vipsname, context related to the sips to be offloaded)
				 String vipsname=msg.GetParam1();
				 String context=msg.GetParam2();
				 
				 int ret=m_cxx.SetContextInfoToVIPS(vipsname, context);
				 
				 if(ret!=0)
				 {
					 logger.error("error in SetContextInfoToVIPS, vipsname="+vipsname+", context="+context); 
					 ReturnError(arg0);
					 return;
				 }
				 
				 ReturnSuccess(arg0); 
			 }
			 else
			 {
				 ReturnUnknown(arg0);
			 }
			 
			 
			 			
		}
		
		
		
	}
	
	//Handler to deal with CODE release process
	public class HReleaseCodePair implements HttpHandler
	{
		public HReleaseCodePair()
		{
			
		}

		@Override
		public void handle(HttpExchange arg0) throws IOException {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(arg0.getRequestBody()));
			 String msgStr="";
			 String line="";
			 while( (line=bufferedReader.readLine()) != null)
			 {
				 msgStr += line;
			 }
			 NormalMessage msg = NormalMessage.FromJsonString(msgStr);
			 if(null==msg)
			 {
				 logger.error("unable to parse message:"+msgStr);
				 ReturnError(arg0);
				 return;
			 }
			 
			 //the message from CDO is to ask (the requester) for deleting the CODE related port between vSwitch and UFD
			 	//P.S. the release of provider's helping vips is dealt with in TdLocalAdjust
			 if(msg.GetMsgType().equals(NormalMessage.REQ_RELEASE_REQUESTERS_UFD_OVS_PORT))
			 {
				 //param1: tunid, param2:ovs-ufd port
				 int tunid=Integer.parseInt(msg.GetParam1());
				 int port_ufd_ovs_provider=Integer.parseInt(msg.GetParam2());
				 if(0==m_cxx.ReleaseCodeOvsCommand(tunid, port_ufd_ovs_provider))
				 {
					 //delete the record
					 m_cxx.RmRecordForeignHelpingVipsPortInfo(port_ufd_ovs_provider);
					 ReturnSuccess(arg0);
				 }
				 else
				 {
					 logger.error("error in release ufd-ovs port"+port_ufd_ovs_provider);
					 ReturnError(arg0);
				 }
			 }
			 else
			 {
				 ReturnUnknown(arg0);
			 }
			
		}
		
	}
	
	
	
	public void ReturnError(HttpExchange exchange) throws IOException
	{
		String response = UrlType.RESP_FAIL;
        exchange.sendResponseHeaders(200, 0);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
	}
	
	public void ReturnSuccess(HttpExchange exchange) throws IOException
	{
		String response = UrlType.RESP_SUCCESS;
        exchange.sendResponseHeaders(200, 0);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
	}
	
	public void ReturnUnknown(HttpExchange exchange) throws IOException
	{
		String response = UrlType.RESP_UNKNOWN;
        exchange.sendResponseHeaders(200, 0);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
	}
	
	public void ReturnInfo(HttpExchange exchange,String msg) throws IOException
	{
        exchange.sendResponseHeaders(200, 0);
        OutputStream os = exchange.getResponseBody();
        os.write(msg.getBytes());
        os.close();
	}
	
	public boolean GetDebugFlag()
	{
		return this.flag_debug;
	}

	
	@Override
	public void run()
	{
		logger.info("Thread TdCdoCommandExecutor launches");
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(m_port), 0);
			
			server.createContext(UrlType.SUFFIX_CXX_CODE_PAIR_INIT, new HNewCodePair());
			server.createContext(UrlType.SUFFIX_CXX_CODE_PAIR_RELEASE, new HReleaseCodePair());
			server.setExecutor(null); // creates a default executor
			server.start();
			
			this.flag_debug=true;
			
			while(!this.flag_stop)
			{
				Thread.sleep(100);
			}
			
			server.stop(1);
			logger.info("Thread TdCdoCommandExecutor ends");
					
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
