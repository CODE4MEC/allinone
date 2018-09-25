package net.floodlightcontroller.cdo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionPopVlan;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.cdo.types.Alarm;
import net.floodlightcontroller.cdo.types.AverageVStats;
import net.floodlightcontroller.cdo.types.ContainerStats;
import net.floodlightcontroller.cdo.types.DefaultValues;
import net.floodlightcontroller.cdo.types.NodeOnOffState;
import net.floodlightcontroller.cdo.types.NodeStats;
import net.floodlightcontroller.cdo.types.NodeStats_S;
import net.floodlightcontroller.cdo.types.NormalMessage;
import net.floodlightcontroller.cdo.types.UrlType;
import net.floodlightcontroller.cdo.types.VipsTrafficInfo;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.SwitchMessagePair;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.staticentry.IStaticEntryPusherService;
import net.floodlightcontroller.util.ConcurrentCircularBuffer;
import net.floodlightcontroller.util.FlowModUtils;

public class CDO implements IFloodlightModule, IOFMessageListener, CdoService {

	protected IFloodlightProviderService floodlightProvider;
	protected ConcurrentCircularBuffer<SwitchMessagePair> buffer;
	protected IRestApiService restApi;
	protected IStaticEntryPusherService static_entry_pusher;
	protected static Logger logger;
	protected OFFactory factory_of;// = OFFactories.getOFFactory(OFVersion.OF_13);
	protected IOFSwitchService switchService;
	
	//for cdo
	private final long LEN_TIMESLOT=10; //
	private final String DB_NAME="DevDB";
	
//	private final int MAX_MEC_NUM=10;//
	
	Hashtable<String,NodeStats_S> m_ht_node_states=new Hashtable<String, NodeStats_S>();
	Hashtable<String,String> m_ht_providers=new Hashtable<String, String>();
	Hashtable<String,String> m_ht_requesters=new Hashtable<String, String>();
	
	//
	Hashtable<String,Integer> m_ht_name_sid=new Hashtable<String,Integer>();
	
	boolean if_newslot=false;//



	
//	ConcurrentLinkedQueue<Integer> m_sidqueue_avail=new ConcurrentLinkedQueue<Integer>();
	
	//
	Thread m_th_schedule;
	private boolean flag_schedule=false;
	@Override
	public boolean GetScheduleFlag()
	{
		return this.flag_schedule;
	}
	
	public void SetScheduleFlag(boolean flag)
	{
		this.flag_schedule=flag;
	}
	
	@Override
	public String getName() {
		return "CDO";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch(msg.getType()) {
        case PACKET_IN:
            buffer.add(new SwitchMessagePair(sw, msg));
            return Command.STOP;
       default:
           break;
    }
    return Command.CONTINUE;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(CdoService.class);
	    return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	    m.put(CdoService.class, this);
	    return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IFloodlightProviderService.class);
	    l.add(IRestApiService.class);
	    l.add(IStaticEntryPusherService.class);
	    l.add(IOFSwitchService.class);
	    return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		restApi = context.getServiceImpl(IRestApiService.class);
		static_entry_pusher=context.getServiceImpl(IStaticEntryPusherService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		
		buffer = new ConcurrentCircularBuffer<SwitchMessagePair>(SwitchMessagePair.class, 100);
		logger = LoggerFactory.getLogger(CDO.class);
		factory_of= OFFactories.getFactory(OFVersion.OF_13);
		
		//DB related
		//DB_NAME="DevDB"//+System.currentTimeMillis();
		try {
			DBTools.CreatingDB(DB_NAME);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return;
		}
		
		
		//test 
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				System.out.println("Pos 1");
//				try {
//					boolean flag=false;
//					while(!flag)
//					{
//						Thread.sleep(1000);
//						Set<DatapathId> dpids=switchService.getAllSwitchDpids();
//						if(dpids.size()>0)
//						{
//							flag=true;
//							//Test_FlowAdd("00:0c:29:ec:90:5c");
//							Test_FlowAdd("00:00:18:66:da:f2:ee:4d");
//							System.out.println("[success] add flows");
//						}					
//					}
//					System.out.println("Pos 2");
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					System.out.println("Pos error");
//					e.printStackTrace();
//				}
//			}
//			
//		}).start();
		

	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		restApi.addRestletRoutable(new CdoRoutable());
		
		
		
	}

	@Override
	public String Test() {
		String ret="success";
		return ret;
	}

	@Override
	public int UpdateStats(List<ContainerStats> l) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int CollectingSchedulingRequests(Alarm alarm) {
		int ret=0;
		String nid=alarm.GetNid();
		
		if(this.m_ht_node_states.containsKey(nid))
		{
			
			//将解析到的alarm节点置入requester ht 中
			this.m_ht_providers.remove(nid);
			this.m_ht_requesters.put(nid, nid);
			
		}
		else
		{
			//unknown nid
			ret=1;
		}
		
		return ret;
	}


	@Override
	public int UpdateOnOffState(NodeOnOffState onoff_state) {
		int ret=0;
		
		String nodename=onoff_state.GetNid();
		int istate=onoff_state.GetOnOffState();
		switch (istate)
		{
		case NodeOnOffState.STATE_ON:
			//
			if(!this.m_ht_node_states.containsKey(nodename))
			{
//				int sid=AllocateSID(nodename);
//				//
//				if(sid<0)
//				{
//					ret=-1;
//					break;
//				}
				NodeStats_S nss=new NodeStats_S(onoff_state);
				this.m_ht_node_states.put(nodename, nss);
				this.m_ht_providers.put(nodename, " ");
				//debug
				System.out.println("Add Node:"+nodename);
				
				//
				FlowAdd4Init(onoff_state);
				System.out.println("[received]"+onoff_state.GetOvsMacAddr());
				
//				//TEST:
//				String urltest="http://"+onoff_state.GetIpAddr()+":"+UrlType.CXX_API_PORT+UrlType.SUFFIX_CXX_NEW_HELPING_CONTAINER;
//				this.SendMessageWithPost(urltest, "this is a testing message from CDO!");
//				//ETEST
							
			}
			break;
			
		case NodeOnOffState.STATE_OFF:
			//
			if(this.m_ht_node_states.containsKey(nodename))
			{
				this.m_ht_node_states.remove(nodename);
				this.m_ht_providers.remove(nodename);
				this.m_ht_requesters.remove(nodename);
				
				//
				//ReleaseSID(nodename); 
				
				
				//debug
				System.out.println("Remove Node:"+nodename);
			}
			break;
			
		default:
			ret = 1;
			break;
		}
		return ret;
	}
	
public void FlowAdd4Init(NodeOnOffState onoff)
{
	String mac_target_switch=onoff.GetOvsMacAddr();
	DatapathId dpid=DatapathId.of(mac_target_switch);

	
	/**/
	static_entry_pusher.deleteEntriesForSwitch(dpid);
	
	/**/
	IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(mac_target_switch));
	ConcurrentLinkedQueue<OFFlowAdd> rules=GetInitFlows(onoff);
	while(!rules.isEmpty())
	{
		OFFlowAdd rule=rules.poll();
		mySwitch.write(rule);
	}
	
}

public ConcurrentLinkedQueue<OFFlowAdd> GetInitFlows(NodeOnOffState onoff) {
	ConcurrentLinkedQueue<OFFlowAdd> rules=new ConcurrentLinkedQueue<OFFlowAdd>();
	

	//
	//	
	OFFlowAdd rule1=factory_of.buildFlowAdd()
		    .setPriority(3)
		    .setMatch(Init_GetMatch(1,onoff))
		    .setActions(Init_GetActionList(1,onoff))
		    .setTableId(TableId.of(0))
			.build();
	rules.add(rule1);
	
	//RULE 2: 
	//	
	OFFlowAdd rule2=factory_of.buildFlowAdd()
		    .setPriority(3)
		    .setMatch(Init_GetMatch(2,onoff))
		    .setActions(Init_GetActionList(2,onoff))
		    .setTableId(TableId.of(0))
			.build();
	rules.add(rule2);
	

	
	//RULE 3:
	//	
	OFFlowAdd rule3=factory_of.buildFlowAdd()
		    .setPriority(3)
		    .setMatch(Init_GetMatch(3,onoff))
		    .setActions(Init_GetActionList(3,onoff))
		    .setTableId(TableId.of(0))
			.build();
	rules.add(rule3);
	
	//RULE 4: 
	//	
	OFFlowAdd rule4=factory_of.buildFlowAdd()
		    .setPriority(3)
		    .setMatch(Init_GetMatch(4,onoff))
		    .setActions(Init_GetActionList(4,onoff))
		    //.setInstructions(Test_GetInstructions(4))
		    .setTableId(TableId.of(0))
			.build();
	rules.add(rule4);
	
	//RULE 5:
//	
//	
//	
	OFFlowAdd rule5=factory_of.buildFlowAdd()
		    .setPriority(1)
		    .setMatch(Init_GetMatch(5,onoff))
		    .setActions(Init_GetActionList(5,onoff))
		    //.setInstructions(Test_GetInstructions(4))
		    .setTableId(TableId.of(0))
			.build();
	rules.add(rule5);
	
	//RULE 6: 
	//
	//match：
	//	arp，
	//	in_port=OFPP_LOCAL，
	//	ARP_OP=ARP_REPLY，
	//	ARP_SPA＝192.168.97.1，
	//	ARP_TPA＝192.168.97.101
	//action：output=OFPP_FLOOD
	OFFlowAdd rule6=factory_of.buildFlowAdd()
		    .setPriority(2)
		    .setMatch(Init_GetMatch(6,onoff))
		    .setActions(Init_GetActionList(6,onoff))
		    //.setInstructions(Test_GetInstructions(4))
		    .setTableId(TableId.of(0))
			.build();
	rules.add(rule6);
	
//	static_entry_pusher.addFlow("rule1", rule1, DatapathId.of(mac_target_switch));
//	static_entry_pusher.addFlow("rule2", rule2, DatapathId.of(mac_target_switch));
//	static_entry_pusher.addFlow("rule3", rule3, DatapathId.of(mac_target_switch));
//	static_entry_pusher.addFlow("rule4", rule4, DatapathId.of(mac_target_switch));
	
	return rules;	
	}

private Match Init_GetMatch(int ruleID, NodeOnOffState onoff) {
	
	String str_ip_vs=onoff.GetVserverIp();
	int port_ovs_vs=onoff.GetOvsVserverPort();
	
	Match myMatch=null;
//	Match myMatch = factory_of.buildMatch()
//		    .setExact(MatchField.IN_PORT, OFPort.of(1))
//		    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//		    .setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("192.168.0.1/24"))
//		    .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
//		    .setExact(MatchField.TCP_DST, TransportPort.of(80))
//		    .build();
	switch(ruleID)
	{
	//CASE 1:
	//Match：
	//	
	//  
	case 1:
		myMatch = factory_of.buildMatch()
		.setExact(MatchField.TUNNEL_ID, U64.of(1001))
		.setExact(MatchField.IPV4_DST, IPv4Address.of(str_ip_vs))
		.setExact(MatchField.ETH_TYPE, EthType.IPv4)
		.build();
		break;
	
	//CASE 2: 
	//Match：In_port:10001
	//目的ip：(vServer)
	case 2:
		myMatch = factory_of.buildMatch()
				.setExact(MatchField.IN_PORT, OFPort.of(10001))
				.setExact(MatchField.IPV4_DST, IPv4Address.of(str_ip_vs))
				.setExact(MatchField.ETH_TYPE, EthType.IPv4)
				.build();
		break;
	
	//CASE 3:
	//MATCH:
	//1. In_port:port_vs
	//2. sIP：vserver
	case 3:
		myMatch = factory_of.buildMatch()
		.setExact(MatchField.IN_PORT, OFPort.of(port_ovs_vs))
		.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of(UrlType.SERVER_IP_WITHMASK))
		//.setMasked(MatchField.IPV4_DST, IPv4AddressWithMask.of(UrlType.USER_IP_WITHMASK))
		.setExact(MatchField.ETH_TYPE, EthType.IPv4)
		.build();
		break;
	
	//CASE 4: 
	//Match：
	//1. In_port:10001
	//2. sip：vserver
	case 4:
		myMatch = factory_of.buildMatch()
		.setExact(MatchField.IN_PORT, OFPort.of(10001))
		.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of(UrlType.SERVER_IP_WITHMASK))
		//.setMasked(MatchField.IPV4_DST, IPv4AddressWithMask.of(UrlType.USER_IP_WITHMASK))
		.setExact(MatchField.ETH_TYPE, EthType.IPv4)
		.build();
		break;
	
	//CASE5:match all/none
	case 5:
		myMatch = factory_of.buildMatch()
		.build();
		break;
		
	//RULE 6: 
	//
	//match：
	//	arp，
	//	in_port=OFPP_LOCAL，
	//	ARP_OP=ARP_REPLY，
	//	ARP_SPA＝192.168.97.1，
	//	ARP_TPA＝192.168.97.101
	//action：output=OFPP_FLOOD	
	case 6:
		myMatch = factory_of.buildMatch()
		.setExact(MatchField.ETH_TYPE, EthType.ARP)
		.setExact(MatchField.IN_PORT, OFPort.LOCAL)
		.setExact(MatchField.ARP_OP, ARP.OP_REPLY)
		.setExact(MatchField.ARP_SPA, IPv4Address.of("192.168.97.1"))
		.setExact(MatchField.ARP_TPA, IPv4Address.of("192.168.97.101"))
		.build();
		break;
	default:
		System.out.println("ERROR UNKNOWN RULE");
		break;	
	}
		return myMatch;
	}

	private ArrayList<OFAction> Init_GetActionList(int ruleID,NodeOnOffState onoff) {

		int port_ovs_vs=onoff.GetOvsVserverPort();
		String ip_up=onoff.GetUserPlaneIp();
		
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		OFActions actions = factory_of.actions();
		OFOxms oxms = factory_of.oxms();
		
		switch(ruleID)
		{
		//CASE 1:
		//Action：
		//  1. ： 10001
		case 1:
		{			
			/* . */
			/* Output to a port is also an OFAction, not an OXM. */
			OFActionOutput output = actions.buildOutput()
					.setMaxLen(0xFFffFFff)
					.setPort(OFPort.of(10001))
					.build();
			actionList.add(output);
		}
			break;
		
		//CASE 2: 
		//ACTION:
		//1. dmac：66:66:66:66:66:67
		//2. Output：vsport

		case 2:
		{
			/* . */
			OFActionSetField setDlDst = actions.buildSetField()
					.setField(
							oxms.buildEthDst()
							.setValue(MacAddress.of("66:66:66:66:66:67"))
							.build()
							)
					.build();
			actionList.add(setDlDst);
			
			/*  */
			
			OFActionOutput output = actions.buildOutput()
					.setMaxLen(0xFFffFFff)
					.setPort(OFPort.of(port_ovs_vs))
					.build();
			actionList.add(output);
			
		}
		break;
			
		//CASE 3: 
		//Output：10001
		case 3:
		{			
			/* . */
			/* Output to a port is also an OFAction, not an OXM. */
			OFActionOutput output = actions.buildOutput()
					.setMaxLen(0xFFffFFff)
					.setPort(OFPort.of(10001))
					.build();
			actionList.add(output);
		}
		break;
		
		//CASE 4: 
		//Action：
		//1. 
		//2. 
		//3. 
		//4. 

		case 4:
		{
			/*  */
			OFActionSetField setTunId = actions.buildSetField()
					.setField(
							oxms.buildTunnelId()
							.setValue(U64.of(1001))
							.build()
							)
					.build();
			actionList.add(setTunId);
			
			/*  */
			OFActionSetField setTunSrc = actions.buildSetField()
					.setField(
							oxms.buildTunnelIpv4Src()
							.setValue(IPv4Address.of(ip_up))
							.build()
							)
					.build();
			actionList.add(setTunSrc);
			
			/* . */
			OFActionSetField setTunDst = actions.buildSetField()
					.setField(
							oxms.buildTunnelIpv4Dst()
							.setValue(IPv4Address.of(UrlType.NODE_IP_USER))
							.build()
							)
					.build();
			
			actionList.add(setTunDst);
			

			
			/* . */
			/* Output to a port is also an OFAction, not an OXM. */
			OFActionOutput output = actions.buildOutput()
					.setMaxLen(0xFFffFFff)
					.setPort(OFPort.of(66))
					.build();
			actionList.add(output);
			
		}
			break;
		//RULE 5:
		//
		//
		//action：output=OFPP_NORMAL
		case 5:
		{
			OFActionOutput output = actions.buildOutput()
					.setPort(OFPort.NORMAL)
					.build();
			actionList.add(output);
		}
			break;
		//RULE 6: 
		//：2
		//match：
		//	arp，
		//	in_port=OFPP_LOCAL，
		//	ARP_OP=ARP_REPLY（），
		//	ARP_SPA＝192.168.97.1，
		//	ARP_TPA＝192.168.97.101
		//action：output=OFPP_FLOOD			
		case 6:
		{
			OFActionOutput output = actions.buildOutput()
					.setPort(OFPort.FLOOD)
					.build();
			actionList.add(output);
		}
			break;
		default:
			System.out.println("ERROR UNKNOWN RULE");
			break;	
		}
				
		return actionList;
	}
	
//	public ArrayList<OFInstruction> Test_GetInstructions(int ruleId)
//	{
//		ArrayList<OFAction> actionList = Test_GetActionList(ruleId);
//		OFInstructions instructions = factory_of.instructions();
//		OFInstructionApplyActions applyActions = instructions.buildApplyActions()
//			    .setActions(actionList)
//			    .build();
//		ArrayList<OFInstruction> myInstructionList = new ArrayList<OFInstruction>();
//	
//		myInstructionList.add(applyActions);
//		return myInstructionList;
//	}

//	//
//	private void ReleaseSID(String nodename) {
//		//1
//		Integer sid = this.m_ht_name_sid.get(nodename); 
//		
//		//
//		if (sid == null)
//		{
//			return;
//		}
//		
//		this.m_ht_name_sid.remove(nodename);
//		//
//		this.m_sidqueue_avail.add(sid);
//		
//		//2. 
//		//TD
//	}

//	private int AllocateSID(String nodename) {
//		Integer ret=0;
//		ret=this.m_sidqueue_avail.poll();
//		if(ret==null)
//		{
//			//
//			ret=-1;
//		}
//		else
//		{
//			//
//			this.m_ht_name_sid.put(nodename, ret);
//		}
//		return ret;
//	}

	public List<OFAction> Test_GetActionList() {
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		  OFActions actions = factory_of.actions();
		  OFOxms oxms = factory_of.oxms();
		  /* Use OXM to modify data layer dest field. */
		  OFActionSetField setDlDst = actions.buildSetField()
		          .setField(
		                  oxms.buildEthDst()
		                  .setValue(MacAddress.of("ff:ff:ff:ff:ff:ff"))
		                  .build()
		)
		          .build();
		  actionList.add(setDlDst);
		  /* Use OXM to modify network layer dest field. */
		  OFActionSetField setNwDst = actions.buildSetField()
		          .setField(
		                  oxms.buildIpv4Dst()
		                  .setValue(IPv4Address.of("255.255.255.255"))
		                  .build()
		)
		          .build();
		  actionList.add(setNwDst);
		  /* Popping the VLAN tag is not an OXM but an OFAction. */
		  OFActionPopVlan popVlan = actions.popVlan();
		  actionList.add(popVlan);
		  /* Output to a port is also an OFAction, not an OXM. */
		  OFActionOutput output = actions.buildOutput()
		          .setMaxLen(0xFFffFFff)
		          .setPort(OFPort.of(1))
		          .build();
		  actionList.add(output);
		return actionList;
	}
	
	private void FlowAdd4Code(String requester_id, String provider_id, int port_helping_vips, int tunid, int port_avail_requester)
	{		
		String str_mac_provider = this.m_ht_node_states.get(provider_id).GetStaticStat().GetOvsMacAddr();
		String str_mac_requester = this.m_ht_node_states.get(requester_id).GetStaticStat().GetOvsMacAddr();
		
	//
		ConcurrentLinkedQueue<OFFlowAdd> flowadds_t=new ConcurrentLinkedQueue<OFFlowAdd>();
		IOFSwitch target_switch = switchService.getSwitch(DatapathId.of(str_mac_provider));
		for(int type=1;type<=2;type++)
		{
			OFFlowAdd rule=GetCodeFlowAdd4Provider(type,tunid,port_helping_vips,provider_id,requester_id);
			target_switch.write(rule);			
			//
			flowadds_t.add(rule);
		}
		//
		this.m_ht_node_states.get(provider_id).AddCodeRules(tunid, flowadds_t);
		
	//
		flowadds_t=new ConcurrentLinkedQueue<OFFlowAdd>();
		target_switch = switchService.getSwitch(DatapathId.of(str_mac_requester));
		for(int type=1;type<=4;type++)
		{
			OFFlowAdd rule=GetCodeFlowAdd4Requester(type,tunid,port_avail_requester,provider_id,requester_id);
			target_switch.write(rule);
			//
			flowadds_t.add(rule);
		}
		//
		this.m_ht_node_states.get(requester_id).AddCodeRules(tunid, flowadds_t);
	}
	
	private OFFlowAdd GetCodeFlowAdd4Provider(int type,int tunid,int port_helping_vips,String id_provider, String id_requester)
	{
		Match match;		
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();;
		OFActions actions = factory_of.actions();;
		OFOxms oxms = factory_of.oxms();;
		OFFlowAdd rule=null;
		
		String ip_up_provider=this.m_ht_node_states.get(id_provider).GetStaticStat().GetUserPlaneIp();
		String ip_up_requester=this.m_ht_node_states.get(id_requester).GetStaticStat().GetUserPlaneIp();
		
		switch(type)
		{
		case 1:
			{
				//Match:
				//	1.：tunid
				match = factory_of.buildMatch()
				.setExact(MatchField.TUNNEL_ID, U64.of(tunid))
				.build();
				//Action:
				//1. 
				//2. 	
				OFActionSetField setDlDst = actions.buildSetField()
						.setField(
								oxms.buildEthDst()
								.setValue(MacAddress.of("66:66:66:66:66:66"))
								.build()
								)
						.build();
				actionList.add(setDlDst);
				OFActionOutput output = actions.buildOutput()
						.setMaxLen(0xFFffFFff)
						.setPort(OFPort.of(port_helping_vips))
						.build();
				actionList.add(output);
				//Rule
				//
				rule=factory_of.buildFlowAdd()
					    .setPriority(3)
					    .setMatch(match)
					    .setActions(actionList)
					    .setTableId(TableId.of(0))
						.build();
				break;
			}
					
		case 2:
			{
				//Match2:
				//1.ip，
				//2.in_port=port_helping_vips
				match = factory_of.buildMatch()
						.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						.setExact(MatchField.IN_PORT, OFPort.of(port_helping_vips))
						.build();
				//Action:
				//
				// 
				//，
				//
				
				/*  */
				OFActionSetField setTunSrc = actions.buildSetField()
						.setField(
								oxms.buildTunnelIpv4Src()
								.setValue(IPv4Address.of(ip_up_provider))
								.build()
								)
						.build();
				actionList.add(setTunSrc);
				
				/*  */
				OFActionSetField setTunDst = actions.buildSetField()
						.setField(
								oxms.buildTunnelIpv4Dst()
								.setValue(IPv4Address.of(ip_up_requester))
								.build()
								)
						.build();
				
				actionList.add(setTunDst);
				
				/*  */
				OFActionSetField setTunId = actions.buildSetField()
						.setField(
								oxms.buildTunnelId()
								.setValue(U64.of(tunid))
								.build()
								)
						.build();
				actionList.add(setTunId);		
				
				/**/
				OFActionOutput output = actions.buildOutput()
						.setMaxLen(0xFFffFFff)
						.setPort(OFPort.of(66))
						.build();
				actionList.add(output);
				//Rule
				//
				rule=factory_of.buildFlowAdd()
					    .setPriority(3)
					    .setMatch(match)
					    .setActions(actionList)
					    .setTableId(TableId.of(0))
						.build();
				break;		
			}
		}
	
		return rule;
	}
	
	private OFFlowAdd GetCodeFlowAdd4Requester(int type,int tunid,int port_avail_requester, String id_provider, String id_requester)
	{		
		Match match;		
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();;
		OFActions actions = factory_of.actions();;
		OFOxms oxms = factory_of.oxms();;
		OFFlowAdd rule=null;
		
		String ip_up_provider=this.m_ht_node_states.get(id_provider).GetStaticStat().GetUserPlaneIp();
		String ip_up_requester=this.m_ht_node_states.get(id_requester).GetStaticStat().GetUserPlaneIp();
		
		switch(type)
		{
		case 1:
			{
				//Match:
				//1.IP
				//2.in_port=port_avail_requester
				match = factory_of.buildMatch()
						.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						.setExact(MatchField.IN_PORT, OFPort.of(port_avail_requester))
						.build();
				//Action:
				// ip_up_requester，
				// ip_up_provider
				// tunid，
				// 66
				
				/*  */
				OFActionSetField setTunSrc = actions.buildSetField()
						.setField(
								oxms.buildTunnelIpv4Src()
								.setValue(IPv4Address.of(ip_up_requester))
								.build()
								)
						.build();
				actionList.add(setTunSrc);
				
				/* */
				OFActionSetField setTunDst = actions.buildSetField()
						.setField(
								oxms.buildTunnelIpv4Dst()
								.setValue(IPv4Address.of(ip_up_provider))
								.build()
								)
						.build();
				
				actionList.add(setTunDst);
				
				/*  */
				OFActionSetField setTunId = actions.buildSetField()
						.setField(
								oxms.buildTunnelId()
								.setValue(U64.of(tunid))
								.build()
								)
						.build();
				actionList.add(setTunId);		
				
				/**/
				OFActionOutput output = actions.buildOutput()
						.setMaxLen(0xFFffFFff)
						.setPort(OFPort.of(66))
						.build();
				actionList.add(output);
				//Rule
				//
				rule=factory_of.buildFlowAdd()
					    .setPriority(3)
					    .setMatch(match)
					    .setActions(actionList)
					    .setTableId(TableId.of(0))
						.build();
				break;
			}
		case 2:
			{
				//Match:
				//1.tun_id=tunid，
				//2.dip:vServer IP
				String ip_vserver=this.m_ht_node_states.get(id_requester).GetStaticStat().GetVserverIp();
				match = factory_of.buildMatch()
						.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						.setExact(MatchField.IPV4_DST, IPv4Address.of(ip_vserver))
						.setExact(MatchField.TUNNEL_ID, U64.of(tunid))
						.build();
				//Action
				//1. 66:66:66:66:66:67，
				//2. (ovs-vserver);
				
				OFActionSetField setDlDst = actions.buildSetField()
						.setField(
								oxms.buildEthDst()
								.setValue(MacAddress.of("66:66:66:66:66:67"))
								.build()
								)
						.build();
				actionList.add(setDlDst);
				int port_ovs_vserver=this.m_ht_node_states.get(id_requester).GetStaticStat().GetOvsVserverPort();
				OFActionOutput output = actions.buildOutput()
						.setMaxLen(0xFFffFFff)
						.setPort(OFPort.of(port_ovs_vserver))
						.build();
				actionList.add(output);
				//Rule
				//3
				rule=factory_of.buildFlowAdd()
					    .setPriority(3)
					    .setMatch(match)
					    .setActions(actionList)
					    .setTableId(TableId.of(0))
						.build();
				break;
			}
		case 3:
			{
				//Match
				//1.tun_id=
				//2.(server)
				match = factory_of.buildMatch()
						.setExact(MatchField.TUNNEL_ID, U64.of(tunid))
						.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						.setMasked(MatchField.IPV4_DST, IPv4AddressWithMask.of(UrlType.SERVER_IP_WITHMASK))
						.build();
				
				//Action
				//
				//
				OFActionSetField setDlDst = actions.buildSetField()
						.setField(
								oxms.buildEthDst()
								.setValue(MacAddress.of("66:66:66:66:66:67"))
								.build()
								)
						.build();
				actionList.add(setDlDst);
				
				int port_ovs_vserver=this.m_ht_node_states.get(id_requester).GetStaticStat().GetOvsVserverPort();
				OFActionOutput output = actions.buildOutput()
						.setMaxLen(0xFFffFFff)
						.setPort(OFPort.of(port_ovs_vserver))
						.build();
				actionList.add(output);
				
				//Rule
				//
				rule=factory_of.buildFlowAdd()
					    .setPriority(3)
					    .setMatch(match)
					    .setActions(actionList)
					    .setTableId(TableId.of(0))
						.build();
				break;
			}	
		case 4:
			{
				//Match
				//
				//
				match = factory_of.buildMatch()
						.setExact(MatchField.TUNNEL_ID, U64.of(tunid))
						.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						//.setMasked(MatchField.IPV4_DST,  IPv4AddressWithMask.of(UrlType.USER_IP_WITHMASK))
						.setMasked(MatchField.IPV4_SRC,  IPv4AddressWithMask.of(UrlType.SERVER_IP_WITHMASK))
						.build();
				//Actions
				//
				//
				//
				//
				
				/*  */
				OFActionSetField setTunSrc = actions.buildSetField()
						.setField(
								oxms.buildTunnelIpv4Src()
								.setValue(IPv4Address.of(ip_up_requester))
								.build()
								)
						.build();
				actionList.add(setTunSrc);
				
				/*  */
				OFActionSetField setTunDst = actions.buildSetField()
						.setField(
								oxms.buildTunnelIpv4Dst()
								.setValue(IPv4Address.of(UrlType.NODE_IP_USER))
								.build()
								)
						.build();
				
				actionList.add(setTunDst);
				
				/* */
				OFActionSetField setTunId = actions.buildSetField()
						.setField(
								oxms.buildTunnelId()
								.setValue(U64.of(1001))
								.build()
								)
						.build();
				actionList.add(setTunId);		
				
				/**/
				OFActionOutput output = actions.buildOutput()
						.setMaxLen(0xFFffFFff)
						.setPort(OFPort.IN_PORT)
						.build();
				actionList.add(output);
				
				//Rule
				//
				rule=factory_of.buildFlowAdd()
					    .setPriority(3)
					    .setMatch(match)
					    .setActions(actionList)
					    .setTableId(TableId.of(0))
						.build();
				break;
			}
		}
		
		return rule;
	}
	
	

	@SuppressWarnings("unchecked")
	private Hashtable<String,String> Scheduling() throws IOException
	{
		Hashtable<String,String> ret=new Hashtable<String,String>();
		Long start_time=System.currentTimeMillis();
		if(this.m_ht_requesters.isEmpty())
		{
			return ret;
		}
		
		//
		CheckProviders();
		
		//
		if(this.m_ht_providers.isEmpty())
		{
			this.m_ht_requesters.clear();
			return ret;
		}
		
		//
		if( !this.m_ht_requesters.isEmpty())
		{
			start_time=System.currentTimeMillis();
			logger.info("scheduling start at "+start_time);
			
			Hashtable<String,String> requesters;
			Hashtable<String,String> providers;
			
			//
			synchronized(m_ht_requesters)
			{
				synchronized(m_ht_providers)
				{
					requesters = (Hashtable<String, String>) this.m_ht_requesters.clone();
					providers=(Hashtable<String, String>) this.m_ht_providers.clone();
					
				//
					//（
						//
						//
					this.m_ht_requesters.clear();
					//
					for(Iterator<String> it=this.m_ht_node_states.keySet().iterator();it.hasNext();)
					{
						String name = it.next();
						this.m_ht_providers.put(name, " ");
					}
				}
			}
			
			
			
			
		//
			int N_re=requesters.size();
			int M_pr=providers.size();
			
			String[] name_re=new String[N_re];
			String[] name_pr=new String[M_pr];
			int[] N_m_pr= new int[M_pr];
			int[][] x_nm=new int[N_re][M_pr];
			
			//
			int[][] conn=new int[N_re][M_pr];			
			//
			double[][] r_nm=new double[N_re][M_pr];
			
			//		
			int tmp=0;
			for(Iterator<String> it_r =requesters.keySet().iterator();it_r.hasNext();){
				String key_r=it_r.next();
				name_re[tmp]=key_r;
				tmp++;
			}
			//
			synchronized(this.m_ht_node_states)
			{
				tmp=0;
				for(Iterator<String> it_p =providers.keySet().iterator();it_p.hasNext();){
					String key_p=it_p.next();
					name_pr[tmp]=key_p;
					N_m_pr[tmp]= this.m_ht_node_states.get(key_p).GetDynamicStat().GetAvailCNum();
					tmp++;
				}
			}
			
			//
			//
			for(int i=0;i<N_re;i++)
			{
				for(int j=0;j<M_pr;j++)
				{
					conn[i][j]=1;
				}
			}
			//
			for(int i=0;i<N_re;i++)
			{
				for(int j=0;j<M_pr;j++)
				{
					r_nm[i][j]=1;
				}
			}
			
			
			
			//
			//
			for(int n_re=0;n_re<N_re;n_re++)
			{
				double[] bm = new double[M_pr];
				int index=0;
				
				//
				for(int m_pr=0;m_pr<M_pr;m_pr++)
				{
				//
					//
					if(conn[n_re][m_pr]!=1)
					{
						bm[index]=0;
						continue;
					}
					
					//
					if( ! providers.contains(name_pr[m_pr]))
					{
						bm[index]=0;
						continue;
					}
					
					
					int[][] x_nm_with_n=x_nm.clone();
					x_nm_with_n[n_re][m_pr]=1;
					//CalVm(x, index_m, rate, num_vIPS_avail)
					double bm_nm = CalVm(x_nm_with_n,m_pr,r_nm,N_m_pr[m_pr]) - CalVm(x_nm,m_pr,r_nm,N_m_pr[m_pr]);
					
					//
					double t_pow=0;
					for(int m_t=0;m_t<M_pr;m_t++)
					{
						//
						String t_name = name_pr[m_t];
						if(null != providers.get(t_name))
						{
							if(conn[n_re][m_t]==1)
							{
								t_pow ++;
							}
						}
					}
					
					bm_nm=Math.pow(bm_nm, t_pow);
					bm[index]=bm_nm;
				}
				
				//
				int pr_selected = MyRandom(bm);
				x_nm[n_re][pr_selected]=1;
				ret.put(name_re[n_re], name_pr[pr_selected]);
				
			//
				//
				{
					int t_num=0;
					for(int i=0;i<N_re;i++)
					{
						t_num += x_nm[i][pr_selected];
					}			
					if(N_m_pr[pr_selected] <= t_num)
					{
						providers.remove(name_pr[pr_selected]);
						this.m_ht_providers.remove(name_pr[pr_selected]);
					}
				}
				
			}
		
		}	
		Long current=System.currentTimeMillis();
		logger.info("scheduling ends at "+current);
		logger.info("scheduling process takes "+(current-start_time)+" ms");
		DBTools.WriteEntry("duration_scheduling", current-start_time, DefaultValues.DB_MEASUREMENT_TIMES, DB_NAME);
		return ret;
	}
	
	

	// 
	public int CodeEstablishment(String requester_id, String provider_id) throws IOException
	{
		Long start_time=System.currentTimeMillis();
		int ret=0;
		String ip_provider =this.m_ht_node_states.get(provider_id).GetStaticStat().GetIpAddr();
		String ip_requester=this.m_ht_node_states.get(requester_id).GetStaticStat().GetIpAddr();
		
		String name_helping_vips=""; 
		int port_helping_vips=0;
		
		logger.info("code_establish starting at "+start_time);
		
	//1. 
		NodeStats_S stat_p = this.m_ht_node_states.get(provider_id);
		NodeStats_S stat_r = this.m_ht_node_states.get(requester_id);
		int tunid=-1;
		for(int i=1;i<1000;i++)
		{
			if((stat_p.IsTunIdAvail(i))&&(stat_r.IsTunIdAvail(i)))
			{
				tunid = i;
				break;
			}
		}
		if(-1==tunid)
		{
			System.out.println("[error] no tunnel id available "+provider_id );
			return -1;
		}	
	//2. 
	//	
	// 
	// 	 
	//    
		
		NormalMessage req_new_helping_vips=new NormalMessage(NormalMessage.REQ_NEW_HELPING_VIPS,"");
		String t_msg=req_new_helping_vips.ToJsonString();
		
		//for debug
		if(t_msg.isEmpty())
		{
			System.out.println("[debug error] error in parsing json object "+NormalMessage.class);
		}
		
		String msg_ret=SendJson("http://"+ip_provider+":"+UrlType.CXX_API_PORT+UrlType.SUFFIX_CXX_CODE_PAIR_INIT
										  ,t_msg);
		NormalMessage rsp_new_vips=NormalMessage.FromJsonString(msg_ret);
		if(rsp_new_vips==null)
		{
			System.out.println("[error] fail to init new container in "+provider_id );
			return -2;
		}
		name_helping_vips=rsp_new_vips.GetParam1();
		port_helping_vips=Integer.parseInt(rsp_new_vips.GetParam2());
		
		//
		this.m_ht_node_states.get(provider_id).GetDynamicStat().LockOneContainer();
		this.m_ht_node_states.get(provider_id).GetDynamicStat().GetAvailCNum();
	// 
//		NormalMessage req_tunnel_establishment=new NormalMessage(NormalMessage.REQ_TUNNEL_ESTABLISHMENT,""+tunid,ip_provider);
//		t_msg=req_tunnel_establishment.ToJsonString();
//		//for debug
//		if(t_msg.isEmpty())
//		{
//			System.out.println("[debug error] error in parsing json object "+NormalMessage.class);
//		}
//		//provider set up
//		msg_ret=SendMessageWithPost(ip_provider+":"+UrlType.CXX_API_PORT+UrlType.SUFFIX_CXX_CODE_PAIR_INIT
//								   ,t_msg);
//		if(!msg_ret.equals(UrlType.RESP_SUCCESS))
//		{
//			System.out.println("[error] error in tunnel set up in "+provider_id+", releasing");
//			//TD:
//			// release
//			return -3;
//		}
//		//requester set up
//		msg_ret=SendMessageWithPost(ip_requester+":"+UrlType.CXX_API_PORT+UrlType.SUFFIX_CXX_CODE_PAIR_INIT
//								   ,t_msg);
//		if(!msg_ret.equals(UrlType.RESP_SUCCESS))
//		{
//			System.out.println("[error] error in tunnel set up in "+requester_id+", releasing");
//			//TD:
//			// release
//			return -3;
//		}
		
	// 
		NormalMessage req_avail_port=new NormalMessage(NormalMessage.REQ_AVAIL_UFD_VIPS_PORT);
		msg_ret=SendJson("http://"+ip_requester+":"+UrlType.CXX_API_PORT+UrlType.SUFFIX_CXX_CODE_PAIR_INIT
									  	,req_avail_port.ToJsonString());
		NormalMessage rsp_avail_port=NormalMessage.FromJsonString(msg_ret);
		if((rsp_avail_port==null)||(!rsp_avail_port.GetMsgType().equals(NormalMessage.RSP_AVAIL_UFD_VIPS_PORT)))
		{
			System.out.println("[error] unknown to parse rsp_avail_port message:"+rsp_avail_port);			
			return -3;		
		} 
		int port_requester_avail=Integer.parseInt(rsp_avail_port.GetParam1());
		
		
	// 
		FlowAdd4Code(requester_id,provider_id,port_helping_vips,tunid,port_requester_avail);
		// 
			// 
		stat_p.UpdateCodeInfo(requester_id, 
									 tunid, 
									 provider_id+"_"+name_helping_vips, 
									 port_requester_avail);
		stat_r.UpdateCodeInfo( provider_id, 
									 tunid, 
									 provider_id+"_"+name_helping_vips, 
									 port_requester_avail);

	// 
		NormalMessage req_ovs_command = new NormalMessage(NormalMessage.REQ_OVS_COMMAND,""+tunid,""+port_requester_avail);
		msg_ret=SendJson("http://"+ip_requester+":"+UrlType.CXX_API_PORT+UrlType.SUFFIX_CXX_CODE_PAIR_INIT
			  	,req_ovs_command.ToJsonString());
		if(!msg_ret.equals(UrlType.RESP_SUCCESS))
		{
			System.out.println("[error] error rsp_ovs_command message:"+msg_ret);
			return -4;
		}
		
		Long current_time=System.currentTimeMillis();
		logger.info("code_establish ending at "+current_time);
		logger.info("code_establish costs "+(current_time-start_time)+"ms");
		DBTools.WriteEntry("duration_code_establish", (current_time-start_time), DefaultValues.DB_MEASUREMENT_TIMES, DB_NAME);
		
		return ret;
	}
	
	@Override
	public boolean CheckPermissionNReleasePort(String global_vips_id)
	{
		
		//global_vips_id, e.g., Node1_vIPS0
		String[] tokens = global_vips_id.split("\\_");
		String id_provider=tokens[0];		
		int tunid=this.m_ht_node_states.get(id_provider).GetTunid(global_vips_id); 
		
		if(tunid <0)
		{
			return false; 
		}
		
		int port_ufd_ovs_requester=this.m_ht_node_states.get(id_provider).GetOvsUfdPortInR(tunid);
		String id_requester=this.m_ht_node_states.get(id_provider).GetPeerId(tunid);
		String ip_requester=this.m_ht_node_states.get(id_requester).GetStaticStat().GetIpAddr();
		
		
		// 
//		if( 1 > this.m_ht_node_states.get(id_requester).GetDynamicStat().GetAvailCNum())
//		{
//			return false;
//		}
		
		// 
		NormalMessage portdump=new NormalMessage(NormalMessage.REQ_RELEASE_REQUESTERS_UFD_OVS_PORT,""+tunid,""+port_ufd_ovs_requester);
		String strRet=SendJson("http://"+ip_requester+":"+UrlType.CXX_API_PORT+UrlType.SUFFIX_CXX_CODE_PAIR_RELEASE,portdump.ToJsonString());
		if(!strRet.equals(UrlType.RESP_SUCCESS))
		{
			System.out.println("[error]:incorrect answer in REQ_RELEASE_REQUESTERS_UFD_OVS_PORT, ret="+strRet);
			return false;
		}
		
		
		return true;
	}
	
	@Override
	public int ReleaseCodeRulesNRecords(String global_hvipsname)
	{
		//parse
		//globan_hvipsname e.g. Node1_VIPS0
		String[] tokens=global_hvipsname.split("\\_");
		String id_provider=tokens[0];
		
		
		int tunid = this.m_ht_node_states.get(id_provider).GetTunid(global_hvipsname);
		
		// 
		if(tunid < 0)
		{
			return -1;
		}
		
		String id_requester = this.m_ht_node_states.get(id_provider).GetPeerId(tunid);
		String mac_provider=this.m_ht_node_states.get(id_provider).GetStaticStat().GetOvsMacAddr();
		String mac_requester=this.m_ht_node_states.get(id_requester).GetStaticStat().GetOvsMacAddr();
		
	// 
		ConcurrentLinkedQueue<OFFlowAdd> rules_t=this.m_ht_node_states.get(id_provider).GetCodeRules(tunid);
		while(!rules_t.isEmpty())
		{
			OFFlowAdd rule = rules_t.poll();
			ReleaseFlowAdd(mac_provider,rule);
		}
		this.m_ht_node_states.get(id_provider).RemoveCodeRules(tunid);
		
		// 
		rules_t=this.m_ht_node_states.get(id_requester).GetCodeRules(tunid);
		while(!rules_t.isEmpty())
		{
			OFFlowAdd rule = rules_t.poll();
			ReleaseFlowAdd(mac_requester,rule);
		}
		this.m_ht_node_states.get(id_requester).RemoveCodeRules(tunid);
		
	// 
		this.m_ht_node_states.get(id_requester).ReleaseCodeInfo(global_hvipsname);
		this.m_ht_node_states.get(id_provider).ReleaseCodeInfo(global_hvipsname);
		
		return 0;
	}
	
	public void ReleaseFlowAdd(String mac_target_switch, OFFlowAdd rule)
	{
		OFFlowDelete flowDelete = FlowModUtils.toFlowDelete(rule);
		IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(mac_target_switch));
		mySwitch.write(flowDelete);
	}
	
	
	private int MyRandom(double[] bm) {
		int ret=0;
		
		double sum_bm=Sum(bm);
		double thread = sum_bm * Math.random();
		
		double sum_t=0;
		for(int i=0;i<bm.length;i++)
		{
			sum_t += bm[i];
			if(sum_t >= thread)
			{
				ret = i;
				break;
			}
		}
		
		return ret;
	}

	private double Sum(double[] bm) {
		// TODO Auto-generated method stub
		int len=bm.length;
		double ret=0;
		for(int i=0;i<len;i++)
		{
			ret += bm[i];
		}
		return ret;
	}


// 
	private double CalVm(int[][] x_nm, int index_m, double[][] r_nm, int num_avail_vIPS) {
		// 
		
		// 
		final double f_nameda=1;
		final double f_k=1;
		
		double vm=0;
		
		int N=x_nm.length;
		
		// 
		int sum_n=0;
		for(int n=0;n<N;n++)
		{
			sum_n += x_nm[n][index_m];
		}
		
		
		for(int n=0;n<N;n++)
		{
			// 
			if(0==sum_n)
			{
				continue;
			}
			
			// 
			if (1==x_nm[n][index_m]) 
			{
				double u_nm=0;
				
				u_nm  = f_k * r_nm[n][index_m];
				u_nm *= Math.pow(Math.E, f_nameda*num_avail_vIPS);
				u_nm /= sum_n;
				u_nm = Math.log(u_nm);
				
				vm += u_nm;
			}
		}
		
		
		
		return vm;
	}

	@Override
	public int UpdateNodeStats(NodeStats stats) {
		String name=stats.GetNID();
	// ）
		if(! this.m_ht_node_states.containsKey(name))
		{
			return -1;
		}
	// 
		NodeStats_S nss=this.m_ht_node_states.get(name);
		nss.UpdateDynamicStats(stats);
		//this.m_ht_node_states.put(name, stats);
	// 
		// 
		if(stats.GetAvailCNum()>=2)
		{
			// 
			this.m_ht_requesters.remove(name);
			this.m_ht_providers.put(name, " ");
		}
		else
		{
			this.m_ht_providers.remove(name);
		}
		
		return 0;
	}
	
	// 
	private void CheckProviders()
	{
		for(Entry<String, NodeStats_S> entry: this.m_ht_node_states.entrySet()){
			String id=entry.getKey();
			Integer avail_cnum=entry.getValue().GetDynamicStat().GetAvailCNum();
			if(avail_cnum<2)
			{
				this.m_ht_providers.remove(id);
			}
		}
	}
	
//	public static String SendJsonWithPost(String url, String json_msg)
//	{
//		return SendJson(url,json_msg,"POST");
//	}
//	
//	public static String SendJsonWithGet(String url, String json_msg)
//	{
//		return SendJson(url,json_msg,"GET");
//	}
	
	public static String SendJson(String url, String msg)
	{	
		if(!url.contains("http://"))
		{
			url="http://"+url;
		}
		System.out.println("[info]trying to send to "+url+":"+msg);
		
	       HttpURLConnection conn = null; 
	      
	        try {  
	        	//   
	            URL mURL = new URL(url);  
	            //   
	            conn = (HttpURLConnection) mURL.openConnection();  
	  
	            conn.setRequestMethod("POST");// type=GET/POST  
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
//	                Log.i(TAG, "" + responseCode);  
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

	@Override
	public int UpdateAverageVipsStats(AverageVStats avs) {
		int ret=-1;
		try {
			ret=DBTools.WriteAverageVStats(avs, DB_NAME);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	@Override
	public int UpdateVipsTrafficInfo(VipsTrafficInfo vt)
	{
		int ret=-1;
		try {
			ret=DBTools.WriteVipsTrafficInfo(vt, DB_NAME);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	
	@Override
	public void DisableSchedule()
	{
		this.SetScheduleFlag(false);	
	}
	@Override
	public int EnableSchedule() throws InterruptedException
	{
		if(this.m_th_schedule!=null)
		{
			if(this.m_th_schedule.isAlive())
			{
				return -1;
			}		
		}
		this.SetScheduleFlag(true);
		//CDO related
		this.m_th_schedule=new Thread(new Runnable() {
			@Override
			public void run() {
				ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
				
//				 
//				try {
//					Thread.sleep(10000);
//				} catch (InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
				
				
				while(GetScheduleFlag())
				{
					try {
											
						Hashtable<String,String> ht_schedule_result=Scheduling();
						if(ht_schedule_result.isEmpty())
						{
							Thread.sleep(LEN_TIMESLOT);
							continue;
						}
						// 
						for(Iterator<String> it_r =ht_schedule_result.keySet().iterator();it_r.hasNext();){
							String name_re=it_r.next();
							String name_pr=ht_schedule_result.get(name_re);
							
							// 
							// 
							cachedThreadPool.execute(new Runnable() {
								@Override
								public void run() {
									try {
										CodeEstablishment(name_re,name_pr);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}									
								}
							});
							
						
						}
						
						// 
						//Thread.sleep(LEN_TIMESLOT);
					} catch (InterruptedException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				// 
				cachedThreadPool.shutdown();
				System.out.println("[info] Scheduling Thread Off");
			}
			
		});
		// 
		this.m_th_schedule.start();
		System.out.println("[info] Scheduleing Thread Start");
		return 0;
	}

}
