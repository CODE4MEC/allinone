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

//监听线程，周期性监听容器状态并上报
//支持两种监听方式：
//typeIM.周期性监听，利用立刻输出的docker stats --no-stream 命令，每F_LISTEN_PERIOD测一次
//typeCT.持续监听，利用持续输出的docker stats命令，启用专门用于监听的线程，每秒通过blockingqueue获得一次结果
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
	private BlockingQueue<Hashtable<String,String>> m_bq_req_vips;//新增或删除vIPS（本地/外地）申请队列
	private BlockingQueue<String>m_bq_cstats=new LinkedBlockingQueue<String>();//用于接收子线程上报的容器状态信息
	
	
	//存储各个vIPS的状态
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
	//typeIM.周期性监听，利用立刻输出的docker stats --no-stream 命令，每F_LISTEN_PERIOD测一次
	public void run_typeIM() {
		
		logger.info("Thread TdStatListen launches");
				
		while(!this.m_flag_shutdown)
		{			
		//查看状态
			m_ht_vipsstats=this.m_cxx.GetVipsStats();
		
		//信息上报
			//上报当前节点状态
			this.m_cxx.ReportNodeStatsInfo();
			//上报vips平均负载状态
			AverageVStats avs=new AverageVStats(this.m_cxx.GetMecNodeID(),this.m_ht_vipsstats);
			this.m_cxx.ReportAverageVStats(avs);
			
		// 新建/释放vips检查
			//只有在没有新建vips需求的时候才检测是否需要释放vips
			if( !AlarmDetectNInform())
			{
				ReleaseDetectNInform();
			}
			
		
			
	        //等待下一个时隙
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
	//typeCON.持续监听，利用持续输出的docker stats命令，启用专门用于监听的线程，每秒通过blockingqueue获得一次结果
	public void run_typeCT()
	{
		try
		{
			logger.info("Thread TdStatListen launches");
			CStatsListener td_cs_listen=new CStatsListener(this.m_bq_cstats);
			td_cs_listen.start();
			while(!this.m_flag_shutdown)
			{			
			//查看状态
				String raw_cstats=this.m_bq_cstats.take();				
				m_ht_vipsstats=this.m_cxx.GetVipsStatsFromRaw(raw_cstats);
			
			//信息上报
				//上报当前节点状态
				this.m_cxx.ReportNodeStatsInfo();
				//上报vips平均负载状态
				AverageVStats avs=new AverageVStats(this.m_cxx.GetMecNodeID(),this.m_ht_vipsstats);
				this.m_cxx.ReportAverageVStats(avs);
				
			// 新建/释放vips检查
				//只有在没有新建vips需求的时候才检测是否需要释放vips
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
		
		//记录相关
		int num_vips=0;//记录实际vips数
		String name_local_vips_to_release="";
		double overhead_local_vips_to_release=200;
		String name_helping_vips_to_release="";
		double overhead_helping_vips_to_release=200;
		double sum_overhead=0;
		
		//采用Iterator遍历HashMap  
        Iterator<String> it = this.m_ht_vipsstats.keySet().iterator();  
        while(it.hasNext()) {  
        	
            tName = (String)it.next();  
            tStats=this.m_ht_vipsstats.get(tName);
          //double t_overhead = tStats.getCPUPercent()+tStats.getMemPercent();
            double t_overhead = tStats.getCPUPercent();
            
            //本地vips和helping vips有着不同的释放规则
            switch(this.m_cxx.GetWorkingVipsType(tName))
            {
            case ParamDefault.TYPE_LOCAL_VIPS:
	        //记录整体信息    
	            num_vips++;         
	            sum_overhead += t_overhead;         		
	        //记录负载最低的vIPS
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
        
        //本地低载判断标准: 当前N个vips的总overhead能不溢出的装入N-1个容量不超过特定阈值的vips
        //helping vips低载判断标准：单个helpingvips负载低于helping vips阈值
        //if( sum_overhead < ( (num_vips-1)*(ParamDefault.TH_CPU_RELEASE+ParamDefault.TH_MEM_RELEASE) ) )
        if( (sum_overhead < ( (num_vips-1)*(ParamDefault.TH_CPU_RELEASE_LOCAL) ) )
          ||(overhead_helping_vips_to_release <= ParamDefault.TH_CPU_RELEASE_HELPING) )
        {       	
        	lowOccupancyFlag=true;
        }
        
        //当连续TH_RELEASE_SLOT_NUM个时隙都走低时，CheckRelease返回true
        if(CheckRelease(lowOccupancyFlag))
        {
        	//最终释放本地和外地负载更低的那个,相同则优先释放本地
        	String name_final=(overhead_helping_vips_to_release<overhead_local_vips_to_release)?
        				      name_helping_vips_to_release:name_local_vips_to_release;
        	//释放请求，释放负载最低的vIPS
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
		
		//采用Iterator遍历HashMap  
        Iterator<String> it = this.m_ht_vipsstats.keySet().iterator();  
        while(it.hasNext()) {  
            tName = (String)it.next();  
            tStats=this.m_ht_vipsstats.get(tName);
                        
            
//            System.out.println("Name:"+tStats.getName() +"\t"
//            					 +"CPU%:"+tStats.getCPUPercent() +"%\t"
//            					 +"MEM%:"+tStats.getMemPercent() +"%");
            
        //alarm和请求新vIPS相关
            try {
            		//一个时隙只请求一次vIPS
        			if(!msg_req_new_vIPS.isEmpty())
        			{
        				continue;
        			}
            	
            		//记录报警次数，决定是否请求新vIPS（本地/外地）,发送请求
	            if( CheckAlarm(tName,tStats.getAlarmFlag()) )
	            {
	            		msg_req_new_vIPS=tName;
	            		Hashtable<String,String> ht=new Hashtable<String,String>();
	            		ht.put(TdVStatListen.REQ_ADD_VIPS, msg_req_new_vIPS);
	            		this.m_bq_req_vips.put(ht);
	            		
	            		ret=true;
	            		
	            		//记录overload的vips
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

	//连续多个时隙报警才算
	private boolean CheckAlarm(String tName, boolean alarmFlag) {
		int t_slot_num=0;
		int SLOTNUM=(this.type==TYPE_IM)?
					ParamDefault.TH_ALARM_SLOT_NUM_TYPE_IM:
					ParamDefault.TH_ALARM_SLOT_NUM_TYPE_CT;
		
		//报警仅对本地vips起作用，不管协作的vips
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
			/*清空报警信息*/
			this.m_ht_alarmcount.put(tName, 0);
			return true;
		}
		
		return false;
	}
	
	//连续多个时隙走低才算
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
