package net.floodlightcontroller.cdo.types;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;

// 
// 
// 
public class NodeStats_S
{
	private NodeOnOffState m_static_stat=null;
	private NodeStats m_dynamic_stat=null;
	//<tunid，peer_id>
	private Hashtable<Integer,String> m_ht_tunid_peerid=new Hashtable<Integer,String>();	
	
	//as provider: <helpingvipsname, tunid>
	private Hashtable<String, Integer> m_ht_hvips_tunid=new Hashtable<String,Integer>();
	
	//as requester: <tunid, port_ovs_ufd>
	private Hashtable<Integer,Integer> m_ht_tunid_portovsufd=new Hashtable<Integer,Integer>();
	
	//<tunid, queue<rules> >
	private Hashtable<Integer,ConcurrentLinkedQueue<OFFlowAdd>> m_ht_code_flowadds = new Hashtable<Integer,ConcurrentLinkedQueue<OFFlowAdd>>();
	
	public NodeStats_S(NodeOnOffState basic_stat)
	{
		this.m_static_stat=basic_stat;
		this.m_dynamic_stat=new NodeStats();
	}
	
	public int UpdateDynamicStats(NodeStats ns)
	{
		this.m_dynamic_stat=ns;
		return 0;
	}
	
	public NodeOnOffState GetStaticStat()
	{
		return this.m_static_stat;
	}
	
	public NodeStats GetDynamicStat()
	{
		return this.m_dynamic_stat;
	}
	
	public int GetTunid(String global_hvips_name)
	{
		if((this.m_ht_hvips_tunid).containsKey(global_hvips_name))
		{
			return this.m_ht_hvips_tunid.get(global_hvips_name);
		}
		else
		{
			System.out.println("[error] unknown global_hvips_name:"+global_hvips_name);
			return -1;
		}
	}
	
	public String GetPeerId(int tunid)
	{
		if( this.m_ht_tunid_peerid.containsKey(tunid) )
		{
			return this.m_ht_tunid_peerid.get(tunid);
		}
		else
		{
			System.out.println("[error] GetPeerId: unknown tunid:"+tunid);
			return "";
		}
	}
	
	public int GetOvsUfdPortInR(int tunid)
	{
		if( this.m_ht_tunid_portovsufd.containsKey(tunid) )
		{
			return this.m_ht_tunid_portovsufd.get(tunid);
		}
		else
		{
			System.out.println("[error] GetOvsUfdPortInR: unknown tunid :"+tunid);
			return -1;
		}
	}
	
//	public void UpdateProviderInfo(String providerName, int tunid, String hvipsName)
//	{
//		this.m_ht_hvips_tunid.put(hvipsName, tunid);
//		this.m_ht_tunid_peerid.put(tunid, providerName);
//	}
//	
//	public void UpdateRequesterInfo(String requesterName, int tunid)
//	{
//		this.m_ht_tunid_peerid.put(tunid, requesterName);
//	}
	
	public void UpdateCodeInfo(String peerName, int tunid, String global_hvipsName, int port_ovs_ufd)
	{
		this.m_ht_tunid_peerid.put(tunid, peerName);
		this.m_ht_hvips_tunid.put(global_hvipsName, tunid);
		this.m_ht_tunid_portovsufd.put(tunid, port_ovs_ufd);
			
	}
	
	public void ReleaseCodeInfo(String global_hvipsName)
	{
		int tunid=this.m_ht_hvips_tunid.get(global_hvipsName);
		
		this.m_ht_hvips_tunid.remove(global_hvipsName);
		this.m_ht_tunid_peerid.remove(tunid);
		this.m_ht_tunid_portovsufd.remove(tunid);
	}
	
//	public void UpdateTunId(int tunid,String peerName)
//	{
//		//globalVipsName=NodeId+vipsName
//		if(!IsTunIdAvail(tunid))
//		{
//			System.out.println("[warning] tunid "+tunid+" is already in use!");
//			return;
//		}	
//			
//		this.m_ht_avail_tun_id.put(tunid, peerName);	
//	}
//	public void ReleaseTunId(int id)
//	{
//		this.m_ht_avail_tun_id.remove(id);
//	}
	
	public boolean IsTunIdAvail(int tunid)
	{
		return !this.m_ht_tunid_peerid.containsKey(tunid);
	}
	
	public void AddCodeRules(Integer tunid,ConcurrentLinkedQueue<OFFlowAdd>rules)
	{
		this.m_ht_code_flowadds.put(tunid, rules);
	}
	
	public void RemoveCodeRules(Integer tunid)
	{
		this.m_ht_code_flowadds.remove(tunid);
	}
	
	public ConcurrentLinkedQueue<OFFlowAdd> GetCodeRules(int tunid)
	{
		return this.m_ht_code_flowadds.get(tunid);
	}
	
	
}
