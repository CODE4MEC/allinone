/*
 * The thread for monitoring the information of vIPS
 * Two monitoring types are supported
 * 1. typeIM, the reading process is returned immediantely, 
 * 		read the vIPS information every $F_LISTEN_PERIOD$ ms 
 * 		based on "docker stats --no-stream"
 * 2. typeCT, the reading process is never returned
 * 		read the vIPS information continuously, the result is queued with blocking queue
 * 		based on "docker stats"
 */
package com.iie.devy.Threads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.iie.devy.Cxxpure.CXX;
import com.iie.devy.Cxxpure.Default.ParamDefault;
import com.iie.devy.Cxxpure.Types.msgtype.AverageVStats;
import com.iie.devy.Cxxpure.Types.msgtype.VipsStats;


public class TdVStatListen extends Thread
{	
	private static final Logger logger=Logger.getLogger(TdVStatListen.class);

	public static final int TYPE_IM=1;
	public static final int TYPE_CT=2;
	
	private int type=TYPE_IM;
	
	private boolean m_flag_shutdown=false;
	
	public static final String REQ_ADD_VIPS="REQ_ADD_VIPS";
	public static final String REQ_REL_VIPS="REQ_REL_VIPS";
	public static final String REQ_SHUT_DOWN="REQ_SHUT_DOWN";
	
	private CXX m_cxx;
	private BlockingQueue<Hashtable<String,String>> m_bq_req_vips;//queuing the requests for adding/deleting new local/foreign-aid vIPS
	private BlockingQueue<String>m_bq_cstats=new LinkedBlockingQueue<String>();//queuing the container information from sub-thread
	
	
	//storing the status information of each vIPS
	private Hashtable<String,VipsStats> m_ht_vipsstats=null;
	private Hashtable<String,Integer> m_ht_alarmcount=new Hashtable<String,Integer>();
	private int count_lowoccupant_slots=0;
	
	@SuppressWarnings("unused")
	private int debug_tsnum=1;
	
	
//		public TdStatListen(BlockingQueue<String> bq, CXX cxx)
//		{
//			this.m_bqStatsToComm=bq;
//			this.m_cxx=cxx;
//		}
	
	public TdVStatListen(CXX cxx, BlockingQueue<Hashtable<String,String>> bq, int listen_type)
	{
		this.m_cxx=cxx;
		this.m_bq_req_vips=bq;
		this.type=listen_type;
		if( (listen_type!=TYPE_CT)&&(listen_type!=TYPE_IM))
		{
			logger.error("unknown type in TdStatListen");
		}
	}
	
	private class CStatsListener extends Thread
	{
		private BlockingQueue<String> m_bq;
		private boolean stopflag=false;
		
		public CStatsListener(BlockingQueue<String> bq)
		{
			this.m_bq=bq;
		}
		public void Stop()
		{
			this.stopflag=true;
		}
		
		
		public void run()
		{
			logger.info("TdCStatsListener ON");
			try {
				String line="";
				String block="\n";
				Process p=Runtime.getRuntime().exec("docker stats");
				//读取命令的输出信息
				InputStream is = p.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				
				while ((line = reader.readLine()) != null) {
					if(this.stopflag)
					{
						break;
					}
					
					if(line.contains("CONTAINER ID"))
					{
						this.m_bq.put(block);
						block = line+"\n";
					}
					else
					{
						block += line+"\n";
					}
				}
				
			} catch (InterruptedException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.info("TdCStatsListener OFF");
		}
	}

	@Override
	public void run()
	{
		switch(this.type)
		{
		case TYPE_IM:
			run_typeIM();
			break;
		case TYPE_CT:
			run_typeCT();
			break;
		}
	}

/* * 1. typeIM, the reading process is returned immediantely, 
 * 		read the vIPS information every $F_LISTEN_PERIOD$ ms 
 * 		based on "docker stats --no-stream"
 */
	public void run_typeIM() {
		
		logger.info("Thread TdStatListen launches");
				
		while(!this.m_flag_shutdown)
		{			
		//check the status
			m_ht_vipsstats=this.m_cxx.GetVipsStats();
		
		//upload information
			//upload the status of current nodes
			this.m_cxx.ReportNodeStatsInfo();
			//upload the average load of vIPSs
			AverageVStats avs=new AverageVStats(this.m_cxx.GetMecNodeID(),this.m_ht_vipsstats);
			this.m_cxx.ReportAverageVStats(avs);
			
		// check for generating/releasing vIPSs request
			if( !AlarmDetectNInform())
			{
				ReleaseDetectNInform();
			}
			
		
			
	        //wait for the next time slot
			try {
				Thread.sleep(ParamDefault.SAMPLE_PERIOD_TYPE_IM);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//debug
			debug_tsnum++;
		}
		logger.info("Thread TdStatListen ends");
		
	}
	
	/*
	 * 2. typeCT, the reading process is never returned
	 * 		read the vIPS information continuously, the result is queued with blocking queue
	 * 		based on "docker stats"
	 */	public void run_typeCT()
	{
		try
		{
			logger.info("Thread TdStatListen launches");
			CStatsListener td_cs_listen=new CStatsListener(this.m_bq_cstats);
			td_cs_listen.start();
			while(!this.m_flag_shutdown)
			{			
			//check the status
				String raw_cstats=this.m_bq_cstats.take();				
				m_ht_vipsstats=this.m_cxx.GetVipsStatsFromRaw(raw_cstats);
			
			//upload information
				//upload the status of current nodes
				this.m_cxx.ReportNodeStatsInfo();
				//upload the average load of vIPSs
				AverageVStats avs=new AverageVStats(this.m_cxx.GetMecNodeID(),this.m_ht_vipsstats);
				this.m_cxx.ReportAverageVStats(avs);
				
				// check for generating/releasing vIPSs request
				if( !AlarmDetectNInform())
				{
					ReleaseDetectNInform();
				}
	
			}
			td_cs_listen.Stop();
			logger.info("Thread TdStatListen ends");
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	

	private void ReleaseDetectNInform() {
		String tName="";
		VipsStats tStats=null;
		boolean lowOccupancyFlag=false;
		
		//Record
		int num_vips=0;//the number of working vIPSs
		String name_local_vips_to_release="";
		double overhead_local_vips_to_release=200;
		String name_helping_vips_to_release="";
		double overhead_helping_vips_to_release=200;
		double sum_overhead=0;
		
		//iterate Hashmap
        Iterator<String> it = this.m_ht_vipsstats.keySet().iterator();  
        while(it.hasNext()) {  
        	
            tName = (String)it.next();  
            tStats=this.m_ht_vipsstats.get(tName);
          //double t_overhead = tStats.getCPUPercent()+tStats.getMemPercent();
            double t_overhead = tStats.getCPUPercent();
            
            //release the vIPS according to its typed
            switch(this.m_cxx.GetWorkingVipsType(tName))
            {
            case ParamDefault.TYPE_LOCAL_VIPS:
	        //summary information    
	            num_vips++;         
	            sum_overhead += t_overhead;         		
	        //record the lowest-loaded vIPS
	            if(t_overhead < overhead_local_vips_to_release)
	            {
	            		name_local_vips_to_release=tName;
	            		overhead_local_vips_to_release = t_overhead;
	            }
	            break;
	            
            case ParamDefault.TYPE_HELPING_VIPS:
            	if(t_overhead<overhead_helping_vips_to_release)
            	{
            		name_helping_vips_to_release=tName;
            		overhead_helping_vips_to_release=t_overhead;
            	}
            	break;
            	
            default:
            	break;
            }
            
        }
        
        /*
         * a local working vIPS is in low load if  ( sum_overhead < ( (num_vips-1)*(ParamDefault.TH_CPU_RELEASE+ParamDefault.TH_MEM_RELEASE) ) )
         * a (local) helping vIPS is in low load if the overhead of the helping vIPS is lower than ParamDefault.TH_CPU_RELEASE_HELPING
        */
        //if( sum_overhead < ( (num_vips-1)*(ParamDefault.TH_CPU_RELEASE+ParamDefault.TH_MEM_RELEASE) ) )
        if( (sum_overhead < ( (num_vips-1)*(ParamDefault.TH_CPU_RELEASE_LOCAL) ) )
          ||(overhead_helping_vips_to_release <= ParamDefault.TH_CPU_RELEASE_HELPING) )
        {       	
        	lowOccupancyFlag=true;
        }
        
        //CheckRelease() will return true if the low loading lasts for TH_RELEASE_SLOT_NUM slots
        if(CheckRelease(lowOccupancyFlag))
        {
        	//Release the vIPS with lowest load
        	//if 2 vIPSs are in the same lowest load, the local vIPS is preferred to be released than the helping vips 
        	String name_final=(overhead_helping_vips_to_release<overhead_local_vips_to_release)?
        				      name_helping_vips_to_release:name_local_vips_to_release;
        	//put the release request in m_bq_req_vips
    		Hashtable<String,String> ht = new Hashtable<String,String>();
    		ht.put(TdVStatListen.REQ_REL_VIPS, name_final);
    		try {
    			this.m_bq_req_vips.put(ht);
    		} catch (InterruptedException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        } 
		
	}

	private boolean AlarmDetectNInform() {
		String tName="";
		VipsStats tStats=null;
		String msg_req_new_vIPS="";
		boolean ret=false;
		
		//iterate HashMap  
        Iterator<String> it = this.m_ht_vipsstats.keySet().iterator();  
        while(it.hasNext()) {  
            tName = (String)it.next();  
            tStats=this.m_ht_vipsstats.get(tName);
                        
            
//            System.out.println("Name:"+tStats.getName() +"\t"
//            					 +"CPU%:"+tStats.getCPUPercent() +"%\t"
//            					 +"MEM%:"+tStats.getMemPercent() +"%");
            
            try {
            		//request at most 1 vIPS per time slot
        			if(!msg_req_new_vIPS.isEmpty())
        			{
        				continue;
        			}
            	
            		//record the times of overloading, and decide whether to request for a new vIPS
	            if( CheckAlarm(tName,tStats.getAlarmFlag()) )
	            {
	            		msg_req_new_vIPS=tName;
	            		Hashtable<String,String> ht=new Hashtable<String,String>();
	            		ht.put(TdVStatListen.REQ_ADD_VIPS, msg_req_new_vIPS);
	            		this.m_bq_req_vips.put(ht);
	            		
	            		ret=true;
	            		
	            		//record the overloaded vips
	            		this.m_cxx.SetOlVips(tName);
	            }		            				        		      			            
            }
            catch(Exception e)
	    		{
	    			logger.error("error in parsing:"+tStats.getInfo());
	    			e.printStackTrace();
	    		}
                     	            
        }
		return ret;
	}

	/*return true only if a local working vIPS is overloaded for a period of time*/
	private boolean CheckAlarm(String tName, boolean alarmFlag) {
		int t_slot_num=0;
		int SLOTNUM=(this.type==TYPE_IM)?
					ParamDefault.TH_ALARM_SLOT_NUM_TYPE_IM:
					ParamDefault.TH_ALARM_SLOT_NUM_TYPE_CT;
		
		//the alarm only works for local working vIPS, regardless of helping vIPS
		if(this.m_cxx.GetWorkingVipsType(tName)!=ParamDefault.TYPE_LOCAL_VIPS)
		{
			return false;
		}
		
		if(alarmFlag)
		{
			if(this.m_ht_alarmcount.containsKey(tName))
			{
				t_slot_num = this.m_ht_alarmcount.get(tName) +1;
				this.m_ht_alarmcount.put(tName, t_slot_num);				
			}
			else
			{
				this.m_ht_alarmcount.put(tName, 1);
				t_slot_num=1;
			}
		}
		else
		{
			if(this.m_ht_alarmcount.containsKey(tName))
			{
				this.m_ht_alarmcount.put(tName, 0);
			}
			t_slot_num=0;
		}
		
		if(t_slot_num >= SLOTNUM)
		{
			/*clear alarm*/
			this.m_ht_alarmcount.put(tName, 0);
			return true;
		}
		
		return false;
	}
	
	//return true only if a local working vIPS is in low load for a period of time
	private boolean CheckRelease(boolean lowOccupancyFlag) {
		int SLOTNUM=(this.type==TYPE_IM)?
				ParamDefault.TH_RELEASE_SLOT_NUM_TYPE_IM:
				ParamDefault.TH_RELEASE_SLOT_NUM_TYPE_CT;
		
		if(lowOccupancyFlag)
		{
			this.count_lowoccupant_slots++;
		}
		else
		{
			this.count_lowoccupant_slots=0;
		}
		
		if(this.count_lowoccupant_slots >= SLOTNUM)
		{
			this.count_lowoccupant_slots=0;
			return true;
		}
		
		return false;
	}
	
	
	
	public void Shutdown()
	{
		this.m_flag_shutdown=true;
	}
	
}
