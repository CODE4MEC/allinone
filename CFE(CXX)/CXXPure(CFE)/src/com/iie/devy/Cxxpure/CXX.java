/*
 * Main class of CXX(i.e., the previous name of CFE)
 */

package com.iie.devy.Cxxpure;

import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.iie.devy.Cxxpure.CommandTools.*;
import com.iie.devy.Cxxpure.Default.ParamDefault;
import com.iie.devy.Cxxpure.Default.UrlType;
import com.iie.devy.Cxxpure.Types.msgtype.AverageVStats;
import com.iie.devy.Cxxpure.Types.msgtype.ContainerStats;
import com.iie.devy.Cxxpure.Types.msgtype.NodeOnOffState;
import com.iie.devy.Cxxpure.Types.msgtype.NodeStats;
import com.iie.devy.Cxxpure.Types.msgtype.NormalMessage;
import com.iie.devy.Cxxpure.Types.msgtype.UfdOffload;
import com.iie.devy.Cxxpure.Types.msgtype.VipsStats;
import com.iie.devy.Cxxpure.Types.msgtype.VipsTrafficInfo;
import com.iie.devy.Threads.*;


import gnu.getopt.Getopt;

public class CXX {
	private final DockerCtrl DCTRL=new DockerCtrl(); //the handle for docker(vIPS)
	private final OvsCtrl OCTRL=new OvsCtrl();//the handle for OVS
	private final UfdCtrl UCTRL=new UfdCtrl();//the handle for UFDE
	private final VServerCtrl VCTRL=new VServerCtrl();//the handle for vServer
	
	
	private String m_id_mecNode; // the id for CXX/MEC node, e.g. "Node1"
	private String m_id_ovs_mac; // MAC address of the local OVS (controlled by CDO)
	
	//URLs for CDO API 
	private String m_url_cdoApi_code_establish;
	private String m_url_cdoApi_code_release;
	private String m_url_cdoApi_nodestats;
	private String m_url_cdoApi_containerstats;
	private String m_url_cdoApi_linkstats;
	private String m_url_cdoApi_onoffstats;
	private String m_url_avs;
	private String m_url_vt;
	

	
	//the number of active container/vips
//	private int m_num_container_running=0;
	
	// Names for available/taken container/vips
	private ConcurrentLinkedQueue<String> q_avail_container_name = new ConcurrentLinkedQueue<String>();
	private ConcurrentLinkedQueue<String> q_taken_container_name = new ConcurrentLinkedQueue<String>();
	private ConcurrentLinkedQueue<String> q_reserved_name = new ConcurrentLinkedQueue<String>(); //Only used in initializing
	
	//Hash tables for ufd and vSwitch to record the information of the connected (local/foreign-aid<helping>) vIPS and the corresponding port number
	private Hashtable<String,Integer> ht_vips_ufd_port=new  Hashtable<String,Integer>();
	private Hashtable<String,Integer> ht_vips_ovs_port=new  Hashtable<String,Integer>();
	
	/*type of working vips（local/helping）*/
	private Hashtable<String, Integer> ht_vips_type=new Hashtable<String,Integer>();
	
	/*message to be sent to CDO when the node is start/shutdown*/
	private String  msg_state_on;
	private String  msg_state_off;
	
	/*the blocking queue used by td_vstatlisten and td_localadjust to communicate*/
	private BlockingQueue<Hashtable<String, String>> bq_statlisten_localadjust;
	
	/*threads*/
	private TdCdoCommandExecutor td_cdocommandexecutor; // the thread to execute the commands from CDO
	private TdLocalAdjust td_localadjust; // the thread to determine to trigger local code, non-local code request, and code release
	private TdVStatListen td_vstatlisten; // the thread to monitor the status of vIPSs
	private TdUStatListen td_ustatlisten; // the thread to monitor the status of UFD
	
	private String ip_local="";
	
	private String id_ol_vips="";
	
	protected final Logger logger = Logger.getLogger(CXX.class);
	
	public void SetOlVips(String id_vips)
	{
		synchronized(this.id_ol_vips)
		{
			this.id_ol_vips=id_vips;
		}		
	}
	
//	private int GetOlVipsPort()
//	{
//		Integer port=this.ht_vips_ufd_port.get(this.id_ol_vips);
//		if(port==null)
//		{
//			System.out.println("[error] ufd port info for "+id_ol_vips+"is not recorded!");
//			return -1;
//		}
//		return 0;
//	}
	
	public CXX(String id_mecNode,String str_LOCAL_IP, String str_OVS_MAC)
	{
		this.m_id_mecNode=id_mecNode;
		this.ip_local=str_LOCAL_IP;
		this.m_id_ovs_mac=str_OVS_MAC;
		
		
		this.ht_vips_ufd_port.put("ufd",ParamDefault.PORT_UFD);
		this.ht_vips_ovs_port.put("ufd",ParamDefault.PORT_UFD);
		
		/*Logger related*/
		//FileAppender appender;
		//HtmlLayout  layout = new HtmlLayout();
		BasicConfigurator.configure();
		logger.setLevel(Level.INFO);
//		try
//        {
//            appender = new FileAppender(layout,"out.txt",false);
//        }catch(Exception e)
//        {            
//        }
//        logger.addAppender(appender);//添加输出端
	}
	
	//Setters and Getters
	public String GetOvsMacAddr()
	{
		return this.m_id_ovs_mac;
	}
	
	// release the information for the released/hung-up vIPS
	public void ReleaseCNameNType(String name)
	{
		if(name!=null && (!name.isEmpty()))
		{
			this.q_taken_container_name.remove(name);
			this.q_avail_container_name.add(name);
			
			this.ht_vips_type.remove(name);
			this.ht_vips_ufd_port.remove(name);
		}
	}
	
	/* generate a unused name, used for the newly activated vIPS*/
	private String GetNewCName()
	{
		String name=this.q_avail_container_name.poll();
		if(!name.isEmpty())
		{
			this.q_taken_container_name.add(name);
		}
		return name;
	}
	
	/*record the UFD port information of each connected vIPS*/
	public void SetUfdVipsPortInfo(String name, int ufdport)
	{
		this.ht_vips_ufd_port.put(name, ufdport);
	}
	
	/*record the vSwitch port information of each connected vIPS*/
	public void SetOvsVipsPortInfo(String name, int ovsport)
	{
		this.ht_vips_ovs_port.put(name, ovsport);
	}
	
	public int GetUfdVipsPortInfo(String vipsName)
	{
		Integer port=this.ht_vips_ufd_port.get(vipsName);
		if(port != null)
		{
			return port;
		}
		
		return -1;
	}
	
	public int GetOvsVipsPortInfo(String vipsName)
	{
		Integer port=this.ht_vips_ovs_port.get(vipsName);
		if(port != null)
		{
			return port;
		}
		
		return -1;
	}
	
	/*Get a available port number that is not used by both UFD and vSwitch*/
	public int GetAvailOvsUfdPort()
	{
		int port=5000;
		ConcurrentLinkedQueue<Integer> ufdPorts=GetUfdPorts();
		ConcurrentLinkedQueue<Integer> ovsPorts=GetOvsPorts();
		
		while(true)
		{
			if((!ufdPorts.contains(port))&&(!ovsPorts.contains(port)))
			{
				break;
			}
			port++;
		}
		
		return port;
	}
	
	public void RmUfdVipsPortInfo(String name)
	{
		this.ht_vips_ufd_port.remove(name);
	}
	
	public void RmOvsVipsPortInfo(String name)
	{
		this.ht_vips_ovs_port.remove(name);
	}
	
	public boolean IsUfdPortNumUsed(int port)
	{
		return this.ht_vips_ufd_port.containsValue(port);
	}
	
//	public boolean IsOvsPortNumUsed(int port)
//	{
//		return this.ht_vips_ovs_port.containsValue(port);
//	}
	
//	public void SetMaxContainerNum(int max)
//	{
//		CXX.m_max_num_container=max;
//	}
	
	public int GetMaxContainerNum()
	{
		return ParamDefault.MAX_VIPS_NUM;
	}
	
	public int GetInitContainerNum()
	{
		return ParamDefault.INIT_VIPS_NUM;
	}
	
//	public void SetRunningContainerNum(int max)
//	{
//		this.m_num_container_running=max;		
//	}
	public int GetRunningContainerNum()
	{
		return this.q_taken_container_name.size();
	}
	
	/*set the url of each CDO API*/
	public void SetUrl(int urltype, String url)
	{
		switch(urltype)
		{
		case UrlType.CDO_CODE_ESTABLISH:
			this.m_url_cdoApi_code_establish=url;
			break;
		case UrlType.CDO_CODE_RELEASE:
			this.m_url_cdoApi_code_release=url;
			break;
		case UrlType.CDO_NODE_STATS:
			this.m_url_cdoApi_nodestats=url;
			break;
		case UrlType.CDO_CONTAINER_STATS:
			this.m_url_cdoApi_containerstats=url;
			break;
		case UrlType.CDO_LINK_STATS:
			this.m_url_cdoApi_linkstats=url;
			break;
		case UrlType.CDO_ONOFF_STATS:
			this.m_url_cdoApi_onoffstats=url;
			break;
		case UrlType.CDO_AVERAGE_VIPS_STATS:
			this.m_url_avs=url;
		case UrlType.CDO_VIPS_TRAFFIC:
			this.m_url_vt=url;
		default:
			break;				
		}
	}
	
	/*get the url of each CDO API*/
	public String GetUrl(int urltype)
	{
		String ret="";
		switch(urltype)
		{
		case UrlType.CDO_CODE_ESTABLISH:
			ret=this.m_url_cdoApi_code_establish;
			break;
		case UrlType.CDO_CODE_RELEASE:
			ret=this.m_url_cdoApi_code_release;
			break;
		case UrlType.CDO_NODE_STATS:
			ret=this.m_url_cdoApi_nodestats;
			break;
		case UrlType.CDO_CONTAINER_STATS:
			ret=this.m_url_cdoApi_containerstats;
			break;
		case UrlType.CDO_LINK_STATS:
			ret=this.m_url_cdoApi_linkstats;
			break;
		case UrlType.CDO_ONOFF_STATS:
			ret=this.m_url_cdoApi_onoffstats;
			break;
		case UrlType.CDO_AVERAGE_VIPS_STATS:
			ret=this.m_url_avs;
			break;
		case UrlType.CDO_VIPS_TRAFFIC:
			ret=this.m_url_vt;
			break;
		default:
			break;				
		}
		return ret;
	}
	
//	public DockerCtrl GetDockerCtrlInstance()
//	{
//		return DCTRL;
//	}
	
	/*obtain the ports used by UFD*/
	private ConcurrentLinkedQueue<Integer> GetUfdPorts()
	{
		return this.UCTRL.GetPorts();
	}
	
	/*obtain the ports used by vSwitch*/
	private ConcurrentLinkedQueue<Integer> GetOvsPorts()
	{
		return this.OCTRL.GetPorts();
	}
	
	/*obtain an available port of UFD*/
	public int GetNewInitUfdPort()
	{
		ConcurrentLinkedQueue<Integer> ports = GetUfdPorts();
		
		while(!ports.isEmpty())
		{
			int port=ports.poll();
			if(!this.IsUfdPortNumUsed(port))
			{
				return port;
			}
		}
		return -1;
	}
	
	/*obtain an available port of vSwitch*/
	public int GetNewInitOvsPort(ConcurrentLinkedQueue<Integer> knownPorts)
	{
		ConcurrentLinkedQueue<Integer> ports = GetOvsPorts();
		
		while(!ports.isEmpty())
		{
			int port=ports.poll();
			if(!knownPorts.contains(port))
			{
				return port;
			}
		}
		return -1;
	}
	
	/* start $initVipsNum$ number of working vIPS from idle vIPSs
	 * Used by:TdlocalAdjust	
	 */	
	private void StartLocalWorkingVIPS(int initVipsNum) {
		for(int i=0;i<initVipsNum;i++)
		{
			StartLocalWorkingVIPS();
		}
	}
	
	/* start 1 working vIPS from idle vIPSs	 */
	public String StartLocalWorkingVIPS()
	{
		String name = "";
		//start a working vIPS only if there are at least 1 idle vIPSs
		if(GetRunningContainerNum() <= (GetMaxContainerNum()-1) )
		{
			//generate a new name for the new working vips
			name = GetNewCName();
			if(name==null || name.isEmpty())
			{
				logger.error("no spare working vips name for local vips");
				return "";
			}
			
			//init the working vips with the new generated name
			int rettmp=this.DCTRL.InitLocalWorkingVIPS(name);
			
			//release the name if failed
			if(rettmp!=0)
			{
				ReleaseCNameNType(name);
				return "";
			}
			
			//record the information of the new vIPS
			this.ht_vips_type.put(name, ParamDefault.TYPE_LOCAL_VIPS);
			
			//get the corresponding port in UFD that is used by the new working vIPS
			int port=this.GetNewInitUfdPort();
			if(port < 0)
			{
				logger.error("cannot parse the newly init ufd port");
				return "";
			}
			//store the port info
			this.SetUfdVipsPortInfo(name, port);
			Date d=new Date();
			logger.debug(d.toString()+" the newly started local working vips "+name+" is binded with port "+port+" in ufd");
		}
		else
		{
			logger.warn("no spare containers");
		}		
		
		return name;
	}
	
	public String GetMaxLoadedVips()
	{
		return this.id_ol_vips;
	}
	
	/*inform UFD to offload the most overloaded vIPS, for detail see case 1 of VOCC
	 * return: the string of SIPs to be offloaded
	 * */
	public String InformUfdToOffload()
	{
		if(this.id_ol_vips.isEmpty())
		{
			return "";
		}
		return InformUfdToOffload(this.id_ol_vips);
	}
	
	/*inform UFD to offload the given vIPS, for detail see case 1 of VOCC
	 * return: the string of SIPs to be offloaded
	 * */
	public String InformUfdToOffload(String overloadVips)
	{
		String url_ufd="http://127.0.0.1:"+UrlType.UFD_API_PORT+UrlType.SUFFIX_UFD_OFFLOAD;
		
		Integer port_ol=this.GetUfdVipsPortInfo(overloadVips);
		if(port_ol<0)
		{
			logger.error("no ufd port for vips:"+overloadVips);
			return "";
		}
		this.id_ol_vips="";
		
		UfdOffload uo=new UfdOffload(port_ol);
		String msg = uo.ToJsonString();
		String ret=CxxTools.SendMessage(url_ufd, msg);
		return ret;
	}
	
	//CXX -> UFD
	/*request UFD for port traffic information*/
	public String GetUfdPortTrafficInfo()
	{
		String url_ufd="http://127.0.0.1:"+UrlType.UFD_API_PORT+UrlType.SUFFIX_UFD_PTINFO;
		String ret=CxxTools.SendMessageWithoutLog(url_ufd, "{}","POST");
		return ret;
	}
	
	//CXX -> CDO
	/*report the vIPS traffic information to CDO*/
	/*not used in current version*/
	public void ReportVipsTrafficInfo(VipsTrafficInfo info)
	{
		String url=this.GetUrl(UrlType.CDO_VIPS_TRAFFIC);
		CxxTools.SendMessageWithoutLog(url, info.ToJsonString(),"POST");
	}
	
	/*Start a helping vIPS for a foreign requester*/
	public String StartHelpingWorkingVIPS()
	{
		String name = "";
		ConcurrentLinkedQueue<Integer> knownPorts;
		//Only start a vIPS when there are idle vIPSs
		if(GetRunningContainerNum() <= (GetMaxContainerNum()-1) )
		{
			/*generate a new name for the vIPS name*/
			name = GetNewCName();
			if(name==null || name.isEmpty())
			{
				logger.error("no spare working vips name for helping vips");
				return "";
			}
			
			//obtain the current ports used by vSwitch
			knownPorts=this.GetOvsPorts();
			
			//start a helping vIPS
			int rettmp=this.DCTRL.InitHelpingWorkingVIPS(name);
			
			//release the name if failed
			if(rettmp!=0)
			{
				ReleaseCNameNType(name);
				return "";
			}
			
			//record the vIPS information
			this.ht_vips_type.put(name, ParamDefault.TYPE_HELPING_VIPS);
			
			//obtain the vSwitch port for the new generated helping vIPS
			int port=this.GetNewInitOvsPort(knownPorts);
			if(port < 0)
			{
				logger.error("cannot parse the newly init ufd port");
				return "";
			}
			//save the port information
			this.SetOvsVipsPortInfo(name, port);
			Date d=new Date();
			logger.debug("[info]"+d.toString()+" the newly started helping working vips "+name+" is binded with port "+port+" in ovs");
		}
		else
		{
			logger.warn("[warning]: no spare containers");
		}		
		
		return name;
				
	}
	
	public int GetWorkingVipsType(String vipsName)
	{
		int ret=ParamDefault.TYPE_UNKNOWN_VIPS;
		
		if(this.ht_vips_type.containsKey(vipsName))
		{
			ret=this.ht_vips_type.get(vipsName);
		}
		
		return ret;
	}
	
	public int OvsInit(String ipCdo)
	{
		return this.OCTRL.OvsInit(ipCdo);
	}
	
	public int UfdInit()
	{
		return this.UCTRL.UfdInit();
	}
	
	public int VServerInit()
	{
		return this.VCTRL.VServerInit();
	}
	
	/* init the idle/reserved vIPSs*/
	public int ReservedVipsInit()
	{
		for(int i=0;i<GetMaxVipsNum();i++)
		{
			String name="vIPS"+i;
			this.q_avail_container_name.add(name);
			this.q_reserved_name.add(name);
		}
		
		//reserved vips init
		ConcurrentLinkedQueue<String> reservedVIPsName=this.q_reserved_name;
		while(reservedVIPsName.size()!=0)
		{ 
			String name=reservedVIPsName.poll();
			//int ret=this.DCTRL.StartReservedVIPS(name, this.GetParamCpus4Vips(), this.GetCpuId4Vips());
			int ret=this.DCTRL.StartReservedVIPS(name, this.GetParamCpus4Vips());

			if(ret != 0)
			{
				return -1;
			}
		}
		return 0;
	}
	
	
	public String GetMecNodeID()
	{
		return this.m_id_mecNode;
	}
	
	/* get the cpus parameter for each vIPS */
	public double GetParamCpus4Vips()
	{
		return ParamDefault.PARAM_CPUS;
	}
	
//	public int GetCpuId4Vips()
//	{
//		return ParamDefault.PARAM_ID_CPU;
//	}
	
	/* obtain the vips status information from string*/
	public Hashtable<String, VipsStats> GetVipsStatsFromRaw(String raw_stats_info)
	{
		Hashtable<String, ContainerStats> hm_c_stats= this.DCTRL.ParseCStatsFromRaw(m_id_mecNode, raw_stats_info);	
		return GetVStatsFromCStats(hm_c_stats);
	}
	
	/* obtain the vips status information*/
	public Hashtable<String, VipsStats> GetVipsStats()
	{
		Hashtable<String, ContainerStats> hm_c_stats= this.DCTRL.StatContainer(m_id_mecNode);
		return GetVStatsFromCStats(hm_c_stats);
	}
	
	/* transform the container status information to vips status information */
	private Hashtable<String, VipsStats> GetVStatsFromCStats(Hashtable<String, ContainerStats> cstats)
	{
		Hashtable<String, VipsStats> vstats=new Hashtable<String, VipsStats>();
		String tName="";
		ContainerStats tCStats=null;
		VipsStats tVStats=null;
		
		// filter the information for vips from containers' information, and rectify the cpu occupancy parameter 
        Iterator<String> it = cstats.keySet().iterator();  
        while(it.hasNext()) {  
            tName = (String)it.next();  
            tCStats=cstats.get(tName);
            
            if(this.IsVIPS(tName))
            {
            	tVStats=new VipsStats(tCStats,this.GetParamCpus4Vips());
            	vstats.put(tName, tVStats);
            }
        }
        return vstats;
	}
	
	private String GetLocalIpString()
	{
//		String ipStr="";
//		try {
//			ipStr=InetAddress.getLocalHost().getHostAddress();
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return ipStr;
		return this.ip_local;
	}
	
	/* used for test */
	public int CxxInitTest(String str_CDO_IP)
	{
	    //URL configuration (CDO related)
		String prefix="http://"+str_CDO_IP+":"+UrlType.CDO_API_PORT;
        this.SetUrl(UrlType.CDO_CONTAINER_STATS, prefix+UrlType.SUFFIX_CDO_CONTAINER_STATS);
        this.SetUrl(UrlType.CDO_NODE_STATS, prefix+UrlType.SUFFIX_CDO_NODE_STATS);
        this.SetUrl(UrlType.CDO_CODE_ESTABLISH, prefix+UrlType.SUFFIX_CDO_CODE_ESTABLISH);
        this.SetUrl(UrlType.CDO_CODE_RELEASE, prefix+UrlType.SUFFIX_CDO_CODE_RELEASE);
        this.SetUrl(UrlType.CDO_LINK_STATS, prefix+UrlType.SUFFIX_CDO_LINK_STATS);
        this.SetUrl(UrlType.CDO_ONOFF_STATS, prefix+UrlType.SUFFIX_CDO_ONOFF_STATS);
        this.SetUrl(UrlType.CDO_AVERAGE_VIPS_STATS, prefix+UrlType.SUFFIX_CDO_AVERAGE_VIPS_STATS);
            
		
     // Thread initialization      
        this.td_cdocommandexecutor=new TdCdoCommandExecutor(this,UrlType.CXX_API_PORT);
                
        //start thread
        this.td_cdocommandexecutor.start();
        
        while(!this.td_cdocommandexecutor.GetDebugFlag())
        {
           
            try {
    			Thread.sleep(100);
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    			return -4;
    		}
        }
        
        CxxTools.SendMessage(prefix+UrlType.SUFFIX_CDO_TEST, "");
        
        
		return 0;
	}
	
	/*Initialization of CXX(CFE) */
	public int CxxInit(String str_CDO_IP) throws InterruptedException
	{
		String str_MAC=GetOvsMacAddr();
		
	    //URL(CDO ralated) configuration
		String prefix="http://"+str_CDO_IP+":"+UrlType.CDO_API_PORT;
        this.SetUrl(UrlType.CDO_CONTAINER_STATS, prefix+UrlType.SUFFIX_CDO_CONTAINER_STATS);
        this.SetUrl(UrlType.CDO_NODE_STATS, prefix+UrlType.SUFFIX_CDO_NODE_STATS);
        this.SetUrl(UrlType.CDO_CODE_ESTABLISH, prefix+UrlType.SUFFIX_CDO_CODE_ESTABLISH);
        this.SetUrl(UrlType.CDO_CODE_RELEASE, prefix+UrlType.SUFFIX_CDO_CODE_RELEASE);
        this.SetUrl(UrlType.CDO_LINK_STATS, prefix+UrlType.SUFFIX_CDO_LINK_STATS);
        this.SetUrl(UrlType.CDO_ONOFF_STATS, prefix+UrlType.SUFFIX_CDO_ONOFF_STATS);
        this.SetUrl(UrlType.CDO_AVERAGE_VIPS_STATS, prefix+UrlType.SUFFIX_CDO_AVERAGE_VIPS_STATS);
        this.SetUrl(UrlType.CDO_VIPS_TRAFFIC, prefix+UrlType.SUFFIX_CDO_VIPS_TRAFFIC);

            
    //Prepare the state_on and state_off message
        NodeOnOffState state_on=new NodeOnOffState(this.GetMecNodeID(),str_MAC,this.GetLocalIpString(),UrlType.IP_UP,UrlType.IP_VServer,ParamDefault.PORT_OVS_VSERVER,NodeOnOffState.STATE_ON);           
        NodeOnOffState state_off=new NodeOnOffState(this.GetMecNodeID(),str_MAC,this.GetLocalIpString(),UrlType.IP_UP,UrlType.IP_VServer,ParamDefault.PORT_OVS_VSERVER,NodeOnOffState.STATE_OFF);

        this.msg_state_on = state_on.ToJsonString();
		this.msg_state_off = state_off.ToJsonString();
        
    // Components (OVS，UFD，VIPS) initialization
//        this.SetMaxContainerNum(10);
//        //ovs initialization
//        if(0!=this.OvsInit(str_CDO_IP))
//        	{
//        		System.out.println("[error] OVS Init Failed!");
//        		return -1;
//        	}
//        //ufd initialization
//        if(0!=this.UfdInit())
//        {
//        		System.out.println("[error] UFD Init Failed!");
//        		return -2;
//        }
		// vIPS initialization
        //start $ParamDefault.MAX_VIPS_NUM$ number of reserved(idle) vIPS
        int ret=this.ReservedVipsInit();
        if(ret != 0)
        {
        		return -100;
        }
        
//        for(int time=20;time>0;time=time-2)
//        {
//        		System.out.println("[debug] waiting for reserved vips to get stable:" + time +"s left");
//        		Thread.sleep(2000);
//        }
        
        // start working(active) vIPSs from reserved(idle) vIPSs
        this.StartLocalWorkingVIPS(GetInitVipsNum());
        
//        if(0!=this.VServerInit())
//        {
//        		System.out.println("[error] VServer Init Failed");
//        }
		
     // Threads initialization      
        //the blocking queue used by threads td_statlisten and td_localadjust for communication
        bq_statlisten_localadjust = new LinkedBlockingQueue<Hashtable<String,String>>();
        //threads
        this.td_cdocommandexecutor=new TdCdoCommandExecutor(this,UrlType.CXX_API_PORT);
        this.td_localadjust=new TdLocalAdjust(this,this.bq_statlisten_localadjust);
        this.td_vstatlisten=new TdVStatListen(this,this.bq_statlisten_localadjust,TdVStatListen.TYPE_CT);
        this.td_ustatlisten=new TdUStatListen(this);
        //start threads
        this.td_vstatlisten.start();
        this.td_localadjust.start();
        this.td_cdocommandexecutor.start();
        this.td_ustatlisten.start();
        
        //wait for threads' initialization
        try {
        	logger.info("waiting for threads to init");
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return -4;
		}
        
        //Inform the CDO with state_on message that the NODE is ready for CODE4MEC
        CxxTools.SendMessage(this.GetUrl(UrlType.CDO_ONOFF_STATS), msg_state_on);
        
		return 0;
	}
	



	private int GetInitVipsNum() {
		// TODO Auto-generated method stub
		return ParamDefault.INIT_VIPS_NUM;
	}
	
	private int GetMaxVipsNum()
	{
		return ParamDefault.MAX_VIPS_NUM;
	}

	/*shutdown the local node*/
	public void CxxQuit()
	{
		logger.info("[info] Start release CXX");
	//Inform the CDO with state_off message that the NODE is shutdown
        CxxTools.SendMessage(this.GetUrl(UrlType.CDO_ONOFF_STATS), msg_state_off);
        
    //close threads
        this.td_vstatlisten.Shutdown();
        this.td_localadjust.Shutdown();
        this.td_cdocommandexecutor.Shutdown();
        this.td_ustatlisten.Shutdown();
        
    //OVS release
//        this.OCTRL.OvsRelease();
        
    //UFD release
//        this.UCTRL.UfdRelease();
        
    //vServer release
//        this.VCTRL.VServerRelease();        
     
    //vIPS release
        //transform all working(active) vIPSs to reserved(idle) vIPSs
        String tname="";
        while( (tname=this.q_taken_container_name.poll()) != null)
        {
        		ReleaseWorkingVIPS(tname);
        }
        
        //release all reserved VIPSs
        while( (tname=this.q_avail_container_name.poll()) != null)
        {
        		ReleaseReservedVIPS(tname);
        }
   
	}
	
	/*releasing of helping vIPS (helping vIPS -> reserved vIPS)*/
	public int ReleaseCodePair4Hvips(String name) throws InterruptedException
	{
		//check permission from CDO
		String cdoapi_coderelease=this.GetUrl(UrlType.CDO_CODE_RELEASE);
		String global_hvips_name=this.GetMecNodeID()+"_"+name;
		NormalMessage coderelease=new NormalMessage(NormalMessage.CODE_RELEASE_P1_CHECK_PERMISSION,global_hvips_name);
		String ret=CxxTools.SendMessage(cdoapi_coderelease, coderelease.ToJsonString());
		if(!ret.equals(UrlType.RESP_SUCCESS))
		{
			logger.warn("cdo does not permit vips:"+name+" to release, ret="+ret);
			return -101;
		}
		
		//CDO has permitted the release. Release after 5 seconds
		Thread.sleep(5*1000);	
		ReleaseWorkingVIPS(name);
		
		//the helping vIPS has been released to reserved vIPS. Inform CDO for the releasing of routing rules
		NormalMessage releaserules=new NormalMessage(NormalMessage.CODE_RELEASE_P2_REQ_DEL_RULES,global_hvips_name);
		ret=CxxTools.SendMessage(cdoapi_coderelease, releaserules.ToJsonString());
		if(!ret.equals(UrlType.RESP_SUCCESS))
		{
			logger.warn("[warning]:cdo does not permit del rules");
			return -102;
		}
		
		return 0;
	}
	
	/*releasing of working vIPS (including local vips and helping vips)*/
	public int ReleaseWorkingVIPS(String name)
	{	
		int ret=0;
		if(!this.q_taken_container_name.contains(name))
		{
			logger.warn("[warning]:invalid working vips name:"+name);
			return -10;
		}
		
		//release the vIPS according to its type
		Integer type=this.ht_vips_type.get(name);
		switch(type)
		{
		case ParamDefault.TYPE_LOCAL_VIPS: // release for local working vIPS;  local working vIPS -> reserved vIPS
			ret=this.DCTRL.ReleaseLocalWorkingVIPS(name);
		break;
		
		case ParamDefault.TYPE_HELPING_VIPS: // release for helping working vIPS; helping vIPS -> reserved vIPS
			ret=this.DCTRL.ReleaseHelpingWorkingVIPS(name);
		break;
		
		default:
			ret=-11;
			logger.error("[error] unknown working vip type to release, vipsname="+name);
		break;
		
		}
		
		//release of vIPS's name
		this.ReleaseCNameNType(name);
		
		return ret;
	}
	
	/*release of reserved vIPS*/
	public int ReleaseReservedVIPS(String name)
	{
		if(!this.q_avail_container_name.contains(name))
		{
			logger.warn("[warning]:invalid reserved vips name:"+name);
			return -10;
		}
		return this.DCTRL.ReleaseReservedVIPS(name);
	}
	

	public static void main(String [] args) throws InterruptedException{
		
		//helping message
		//for launching, e.g., java -jar ./CXXPure.jar -c 0.5 -t 5 -n Node1 -l 10.10.26.190 -L 192.168.0.101 -s 10.10.28.151 -m 18:66:da:f2:ee:4d  -S 192.168.95.101 -p 3

		String str_helpmessage="-n [NodeName]\n"
							  +"-m [MAC ADDRESS] \n"
							  +"-s [CDO IP]\n"
							  +"-S [VSERVER IP]\n"
							  +"-l [Local IP]\n"
							  +"-L [user plane IP]\n"
							  +"-t [Max Vips Number]\n"
							  +"-c [Cpus param]\n"
							  +"-p [port ovs-vserver]\n"
							  +"-H start with helping mode(no local vips)\n"
							  + "-h show help message";
		Getopt testOpt  = new Getopt(args[0], args, "n:m:s:S:l:L:t:c:p:Hh");  
		
		String str_CDO_IP="";
		String str_MAC="";
		String str_Node_Name="";
		String str_LOCAL_IP="";
		
		// parse the parameters
		int res;  
		while( (res = testOpt.getopt()) != -1 ) {  
			switch(res) {  
				case 'l':
					str_LOCAL_IP=testOpt.getOptarg();
					break;
				case 'n':
					str_Node_Name=testOpt.getOptarg();
					break;
				case 's':
					str_CDO_IP = testOpt.getOptarg();  					 
					break;
				case 'S':
					UrlType.IP_VServer=testOpt.getOptarg();
					break;
				case 'L':
					UrlType.IP_UP=testOpt.getOptarg();
					break;
				case 'm':  
					str_MAC = testOpt.getOptarg();  
					break;   
				case 't':
					ParamDefault.MAX_VIPS_NUM = Integer.parseInt(testOpt.getOptarg());				
					break;
				case 'c':
					ParamDefault.PARAM_CPUS = Double.parseDouble(testOpt.getOptarg());
					break;
				case 'p':
					ParamDefault.PORT_OVS_VSERVER=Integer.parseInt(testOpt.getOptarg());
					break;
				case 'H':
					ParamDefault.INIT_VIPS_NUM=0;
					break;
				case 'h':	
				default:  
					System.out.println(str_helpmessage); 
					return;
        		} 		        
		}
//		ParamDefault.INIT_VIPS_NUM = (ParamDefault.MAX_VIPS_NUM==0)?0:1;
		
//	//test temp
//		 CXX cxx=new CXX(str_Node_Name,str_LOCAL_IP);
//		 cxx.CxxInitTest(str_CDO_IP, str_MAC);		              
//       try {
//       		Thread.sleep(60*1000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    // end test temp
		
			
	//Initialization
        CXX cxx=new CXX(str_Node_Name,str_LOCAL_IP,str_MAC);
        int ret=cxx.CxxInit(str_CDO_IP);
//        int ret=cxx.CxxInitTest(str_CDO_IP);
        if(ret != 0)
        {
        		cxx.logger.fatal("error in CXXInit, quiting");
        		cxx.CxxQuit(); 
        		return;
        }
    //hung up the main thread              
        try {
        		Thread.sleep(24*60*60*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
            
    //exit           
        cxx.CxxQuit();            
    }

	/* tell whether the tName is used for any vIPS*/
	private boolean IsVIPS(String tName) {
		if(this.q_taken_container_name.contains(tName))
		{
			return true;
		}
		return false;
	}

	/* command execution, used in CODE establishment */
	public int InitCodeOvsCommand(int tunid, int port) {
		return this.OCTRL.InitCodeOvsCommand(tunid,port);
	}
	
	/* command execution, used in CODE release*/
	public int ReleaseCodeOvsCommand(int tunid, int port)
	{
		return this.OCTRL.ReleaseCodeOvsCommand(tunid,port);
	}
	
	/* report the node status information to CDO */
	public void ReportNodeStatsInfo() {
		String url_ns_update=this.GetUrl(UrlType.CDO_NODE_STATS);
		NodeStats ns=new NodeStats(this);
		CxxTools.SendMessageWithoutLog(url_ns_update, ns.ToJsonString(),"POST");
	}
	
	/*get the total number of local working vIPSs*/
	public int CountLocalVips() {
		int count=0;
		for(Entry<String, Integer> entry: this.ht_vips_type.entrySet()){
			int type=entry.getValue();
			if(type == ParamDefault.TYPE_LOCAL_VIPS)
			{
				count++;
			}
		}
		return count;
	}

	/* report average result of vIPS status to CDO */
	public void ReportAverageVStats(AverageVStats avs) {
		// TODO Auto-generated method stub
		String url=this.GetUrl(UrlType.CDO_AVERAGE_VIPS_STATS);
		CxxTools.SendMessageWithoutLog(url, avs.ToJsonString(),"POST");
	}
	
//	public VipsTrafficInfo GetVipsTrafficInfo()
//	{
//		String raw_info=this.UCTRL.GetPortTrafficInfoRaw();
//		return GetVipsTrafficInfoFromRawString(raw_info);
//	}
	
	/* parse the vips traffic information from UFD message*/
	public VipsTrafficInfo GetVipsTrafficInfoFromRawString(String str_ufd_port_traffic_info)
	{
		if(str_ufd_port_traffic_info.isEmpty())
		{
			return new VipsTrafficInfo();
		}
		Hashtable<Integer,Long> ht_port_traffic=new Hashtable<Integer,Long>();
		Hashtable<String,Long> ht_vips_traffic=new Hashtable<String,Long>();
		String[] lines=str_ufd_port_traffic_info.split("\n");
		String[] tokens;
		//Parse port-traffic string
		for(int i=0;i<lines.length;i++)
		{
			tokens=lines[i].split(" ");
			if(tokens.length!=2)
			{
				continue;
			}
			Integer port = Integer.parseInt(tokens[0]);
			Long traffic = Long.parseLong(tokens[1]);
			ht_port_traffic.put(port, traffic);
		}
		//translate to vips-traffic info
		for(Entry<String, Integer> entry:this.ht_vips_ufd_port.entrySet())
		{
			if(!entry.getKey().contains("vIPS"))
			{
				continue;
			}
			
			String name=this.GetMecNodeID()+entry.getKey();
			Integer port=entry.getValue();
			Long traffic = ht_port_traffic.get(port);
			if(traffic!=null)
			{
				ht_vips_traffic.put(name, traffic);
			}
			else
			{
				logger.warn("[warning] FromUfdPTRawString: missing the traffic info for "+name+" with port "+port);
			}
		}
		return new VipsTrafficInfo(ht_vips_traffic);
	}
	
	/*record the information of the foreign helping vips*/
	public int RecordForeignHelpingVipsPortInfo(int port)
	{
		boolean flag=false;
		String name="";
		for(int i=0;i<ParamDefault.MAX_FOREIGN_HELPING_VIPS;i++)
		{
			name="FHvIPS"+i;
			if(!this.ht_vips_ufd_port.containsKey(name))
			{
				flag=true;
				this.ht_vips_ufd_port.put(name, port);
				break;
			}
		}
		
		if(flag)
		{
			logger.info("a foreign vips "+name+" is binded with ufd port "+port);
			return 0;
		}
		else
		{
			logger.error("RecordForeignHelpingVipsPortInfo: the number of FHVIPS exceeds MAX_FOREIGN_HELPING_VIPS");
			return -1;
		}
	}
	
	/*remove the information of the foreign helping vips*/
	public int RmRecordForeignHelpingVipsPortInfo(Integer port)
	{
		boolean flag=false;
		for(int i=0;i<ParamDefault.MAX_FOREIGN_HELPING_VIPS;i++)
		{
			String name="FHvIPS"+i;
			if(this.ht_vips_ufd_port.containsKey(name))
			{
				if(this.ht_vips_ufd_port.get(name)==port)
				{
					flag=true;
					this.ht_vips_ufd_port.remove(name);
					logger.info("record for "+name+" is released!");
					break;
				}
			}
		} 
		
		if(flag)
		{
			return 0;
		}
		else
		{
			logger.error("RmRecordForeignHelpingVipsPortInfo: no port info for "+port+" is saved!");
			return -1;
		}
				
	}
	
	//dev19
	public String  GetContextInfoFromVIPS(String vips_name, String SIPs)
	{
		return this.DCTRL.GetContextInfoFromVIPS(vips_name, SIPs);
	}
	public int SetContextInfoToVIPS(String vips_name,String contextinfo)
	{
		return this.DCTRL.SetContextInfoToVIPS( vips_name, contextinfo);
	}
	//edev19
}
