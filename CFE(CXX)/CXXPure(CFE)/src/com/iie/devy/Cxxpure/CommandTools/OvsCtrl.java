package com.iie.devy.Cxxpure.CommandTools;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

public class OvsCtrl extends CommandExecution {

	private static Logger logger=Logger.getLogger(OvsCtrl.class);
	public OvsCtrl() {
		super();
	}
	
	public int OvsInit(String ipCdo) 
	{
	//建立ovs交换机
		this.ExecCommand("ovs-vsctl add-br ovs-br1"); 
	//ovs交换机连接到物理网卡
		this.ExecCommand("ovs-vsctl add-port ovs-br1 eno1");
		this.ExecCommand("ip addr flush dev eno1");
		this.ExecCommand("ifconfig ovs-br1 192.168.95.169 netmask 255.255.255.0 broadcast 192.168.95.255 up");
		this.ExecCommand("route add default gw 192.168.95.2 dev ovs-br1");
	//连接到CDO
		this.ExecCommand("ovs-vsctl set-controller ovs-br1 tcp:"+ipCdo+":6633");
		return 0;
	}
	
	//TD
	public int OvsRelease()
	{
		
		return 0;
	}

	public ConcurrentLinkedQueue<Integer> GetPorts() {
		ConcurrentLinkedQueue<Integer> ports=new ConcurrentLinkedQueue<Integer>();
		String portinfo=this.ExecCommand("ovs-ofctl show ovs-br1");
	//parse portinfo
		//e.g.
		//		1(eno2): addr:18:66:da:f2:ee:4d
		//	     config:     0
		//	     state:      0
		//	     current:    1GB-FD COPPER AUTO_NEG
		//	     advertised: 10MB-HD 10MB-FD 100MB-HD 100MB-FD 1GB-HD 1GB-FD COPPER AUTO_NEG AUTO_PAUSE
		//	     supported:  10MB-HD 10MB-FD 100MB-HD 100MB-FD 1GB-HD 1GB-FD COPPER AUTO_NEG
		//	     speed: 1000 Mbps now, 1000 Mbps max
		//	 2(e869f4db5f144_l): addr:e6:89:87:c2:e8:79
		//	     config:     0
		//	     state:      0
		//	     current:    10GB-FD COPPER
		//	     speed: 10000 Mbps now, 0 Mbps max
		//	 66(gre1): addr:ea:02:45:31:e4:06
		//	     config:     0
		//	     state:      0
		//	     speed: 0 Mbps now, 0 Mbps max
		//	 10001(patch1): addr:b6:f1:af:7e:9b:6e
		//	     config:     0
		//	     state:      0
		//	     speed: 0 Mbps now, 0 Mbps max
		//	 LOCAL(ovs-br1): addr:18:66:da:f2:ee:4d
		//	     config:     0
		//	     state:      0
		//	     speed: 0 Mbps now, 0 Mbps max
		//	OFPT_GET_CONFIG_REPLY (xid=0x4): frags=normal miss_send_len=0
		if(portinfo!=null && !portinfo.isEmpty())
		{
			String[] tokens=portinfo.split("\n");
			for(int i=0;i<tokens.length;i++)
			{
				String tInfo=tokens[i];
				if(tInfo.contains("addr:"))
				{
					String[] tTokens=tInfo.split("\\(");
					tInfo=tTokens[0];
					tInfo=tInfo.replaceAll(" ", "");
					int port=-1;
					try {
						port=Integer.parseInt(tInfo);					
					}catch(NumberFormatException e)
					{
						;
					}
					if(port > 0)
					{
						ports.add(port);
					}
				}
			}
		}
		return ports;
	}

	public int InitCodeOvsCommand(int tunid, int port) {
		String patch_name_1="patch"+tunid+"_1";
		String patch_name_2="patch"+tunid+"_2";
		
		//在新建前先删除已有的，防止新建错误
		ReleaseCodeOvsCommand(patch_name_1, patch_name_2);
		
		String command="sudo ovs-vsctl " + 
				"    -- add-port ovs-br1 "+patch_name_1 + 
				"    -- set interface "+patch_name_1+" ofport_request="+port+" type=patch options:peer="+patch_name_2 + 
				"    -- add-port ovs-br2 "+patch_name_2 +
				"    -- set interface "+patch_name_2+" ofport_request="+port+" type=patch options:peer="+patch_name_1;
		
		String str_ret=this.ExecCommand(command);
		if(!str_ret.isEmpty())
		{
			logger.error("error in InitCodeOvsCommand:\n"+command+"\n answer="+str_ret);			
			ReleaseCodeOvsCommand(patch_name_1, patch_name_2);
			return -1;
		}
		
		return 0;
	}
	
	private void ReleaseCodeOvsCommand(String patch_name_1, String patch_name_2)
	{
		String command="sudo ovs-vsctl " + 
				"    -- del-port ovs-br1 "+patch_name_1 + 
				"    -- del-port ovs-br2 "+patch_name_2;
		this.ExecCommandWithoutRet(command);
	}
	
	public int ReleaseCodeOvsCommand(int tunid, int port) {
		String patch_name_1="patch"+tunid+"_1";
		String patch_name_2="patch"+tunid+"_2";
		String command="sudo ovs-vsctl " + 
				"    -- del-port ovs-br1 "+patch_name_1 + 
				"    -- del-port ovs-br2 "+patch_name_2;
		String ret=this.ExecCommand(command);
		if(!ret.isEmpty())
		{
			logger.error("error in executing:\n"+command+"\n ret="+ret);
			return -1;
		}
		
		return 0;
		
	}

}
