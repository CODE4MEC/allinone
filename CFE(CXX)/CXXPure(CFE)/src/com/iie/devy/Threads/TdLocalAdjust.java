/*
 * The thread for deciding the triggering of local CODE, non-local CODE, and CODE release
 * The decision is based on vIPS status
 */
package com.iie.devy.Threads;

import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.iie.devy.Cxxpure.CXX;
import com.iie.devy.Cxxpure.CxxTools;
import com.iie.devy.Cxxpure.Default.ParamDefault;
import com.iie.devy.Cxxpure.Default.UrlType;
import com.iie.devy.Cxxpure.Types.msgtype.Alarm;


public class TdLocalAdjust extends Thread
{
	private static final Logger logger=Logger.getLogger(TdLocalAdjust.class);
	
	private CXX m_cxx;
	private BlockingQueue<Hashtable<String,String>> m_bq_req_new_vips;	//和采集线程（TdStatListen）之间的bq	
	private boolean flag_stop=false;
	private ExecutorService pool;
	
	public TdLocalAdjust(CXX cxx, BlockingQueue<Hashtable<String,String>> bq)
	{
		m_cxx=cxx;
		this.m_bq_req_new_vips=bq;
		pool=Executors.newFixedThreadPool(10);	
	}
	
	public void Shutdown()
	{
		try {
			flag_stop=true;
			
			Hashtable<String,String>ht=new Hashtable<String,String>();
			ht.put(TdVStatListen.REQ_SHUT_DOWN, "");
			this.m_bq_req_new_vips.put(ht);
			
		} catch (InterruptedException e) {
			logger.error("error in shutdown");
			e.printStackTrace();
		}
	}
	
	@Override
	public void run()
	{
		logger.info("Thread TdLocalAdjust launches");
		while(!flag_stop)
		{
			try {
				Hashtable<String, String> ht_req=this.m_bq_req_new_vips.take();
				pool.submit(new Runnable() {
					@Override
					public void run()
					{
						try
						{
							if(ht_req.containsKey(TdVStatListen.REQ_ADD_VIPS))
							{			
								ReqNewVips((String)ht_req.get(TdVStatListen.REQ_ADD_VIPS));			
							}
							else if(ht_req.containsKey(TdVStatListen.REQ_REL_VIPS))
							{
								RelVips((String)ht_req.get(TdVStatListen.REQ_REL_VIPS));
							}
							else if(ht_req.containsKey(TdVStatListen.REQ_SHUT_DOWN))
							{
								flag_stop=true;
							}
							else
							{
								logger.warn("TdLocalAdjust: unknown request type");
							}
						} catch (InterruptedException e)
						{
							logger.error("error 77",e);
							e.printStackTrace();
						}
					}
				});				
							
			} catch (InterruptedException e) {
				logger.error("error 208",e);
				e.printStackTrace();
			}
		}
		pool.shutdown();
		logger.info("Thread TdLocalAdjust ends");
		
	}
	

	private void RelVips(String id_vips_to_release) throws InterruptedException {
		// TODO 
		int type=this.m_cxx.GetWorkingVipsType(id_vips_to_release);
		
		switch(type)
		{
		case ParamDefault.TYPE_HELPING_VIPS:
			this.m_cxx.ReleaseCodePair4Hvips(id_vips_to_release);
			break;
			
		case ParamDefault.TYPE_LOCAL_VIPS:
			//keep at least one local working vIPS
			if(this.m_cxx.CountLocalVips()>1)
			{
				this.m_cxx.ReleaseWorkingVIPS(id_vips_to_release);
			}		
			break;
		}
		
		
	}

	private void ReqNewVips(String id_vips_overload)  {
		//start new local working vIPS if available
		if( this.m_cxx.GetMaxContainerNum() > this.m_cxx.GetRunningContainerNum())
		{
		//local CODE triggering	
			//start new local working vips
			String id_vips_new=this.m_cxx.StartLocalWorkingVIPS();	
			//ask ufd for local offloading (VOCC CASE 1)
			String str_offload_sips=this.m_cxx.InformUfdToOffload(id_vips_overload);
			if(!str_offload_sips.isEmpty())
			{
				String contextinfo=m_cxx.GetContextInfoFromVIPS(id_vips_overload,str_offload_sips);
				int tmp=m_cxx.SetContextInfoToVIPS(id_vips_new,contextinfo);
				if(tmp!=0)
				{
					logger.error("[ReqNewVips] error in setting context info to vIPS\""+id_vips_new+"\", contextinfo="+contextinfo);
				}
			}
			
		} 
		else
		{
		//non-local CODE triggering
			//generate CODE request
			Alarm alarm = new Alarm(this.m_cxx.GetMecNodeID());
			//transfer into json string
			String alarmMsg = alarm.ToJsonString();
			//ask CDO for scheduling
			CxxTools.SendMessage(this.m_cxx.GetUrl(UrlType.CDO_CODE_ESTABLISH),alarmMsg);
		}
		
	}
}
