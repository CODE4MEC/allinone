package com.iie.devy.Cxxpure.CommandTools;

import java.util.concurrent.ConcurrentLinkedQueue;


public class UfdCtrl extends CommandExecution {
	
	public UfdCtrl()
	{
		super();
	}
	public Runtime getRuntime()
	{
		return this.run;
	}
	
	public int UfdInit()
	{
	//建立ufd
		this.ExecCommand("ovs-vsctl add-br ovs-br2");
	//与OVS连接
		this.ExecCommand("ovs-vsctl \\\n" + 
				"    -- add-port ovs-br1 patch1 \\\n" + 
				"    -- set interface patch1 ofport_request=10001 type=patch options:peer=patch2  \\\n" + 
				"    -- add-port ovs-br2 patch2 \\\n" + 
				"    -- set interface patch2 ofport_request=10001 type=patch options:peer=patch1\n" + 
				"");
	//运行ufd应用
		this.ExecCommand("ryu-manager ufd.py");	
	//将UFD连接到ryu控制器
		this.ExecCommand("ovs-vsctl set-controller ovs-br2 tcp:127.0.0.1:6633");
		return 0;
	
	}
	
	//TD:
	public int UfdRelease()
	{
		return 0;
	}
	
	public ConcurrentLinkedQueue<Integer> GetPorts()
	{
		ConcurrentLinkedQueue<Integer> ports=new ConcurrentLinkedQueue<Integer>();
		String portinfo=this.ExecCommand("ovs-ofctl show ovs-br2");
	//parse portinfo
		//e.g.
		//	10(1dff785a341e4_l): addr:46:bd:f2:67:bb:6f
		//	     config:     0
		//	     state:      0
		//	     current:    10GB-FD COPPER
		//	     speed: 10000 Mbps now, 0 Mbps max
		//	 11(b66d6942ad124_l): addr:92:f5:1e:c0:51:6d
		//	     config:     0
		//	     state:      0
		//	     current:    10GB-FD COPPER
		//	     speed: 10000 Mbps now, 0 Mbps max
		//	 10001(patch2): addr:aa:f9:e4:6d:e1:b8
		//	     config:     0
		//	     state:      0
		//	     speed: 0 Mbps now, 0 Mbps max
		//	 LOCAL(ovs-br2): addr:0a:45:00:bb:f0:4d
		//	     config:     PORT_DOWN
		//	     state:      LINK_DOWN
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
	
}

