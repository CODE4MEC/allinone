package com.iie.devy.Cxxpure.CommandTools;

public class VServerCtrl extends CommandExecution {
	
	public VServerCtrl()
	{
		super();
	}
	public Runtime getRuntime()
	{
		return this.run;
	}
	
	public int VServerInit()
	{
	//建立vServer
		this.ExecCommand("docker run --name=server --network=none -d -p 8080:80 server-image");
	//连接到OVS
		this.ExecCommand("ovs-docker add-port ovs-br1 eth0 server "
				+ "--ipaddress=192.168.95.233/24 "
				+ "--gateway=192.168.95.1 "
				+ "--macaddress=66:66:66:66:66:67 "
				+ "--mtu=1300");
		return 0;
	
	}
	
	public int VServerRelease()
	{
	//释放VServer
		this.ExecCommand("docker stop server");
		this.ExecCommand("docker rm server");
		return 0;
	}
	
	
	
}
