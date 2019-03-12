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
	//init vServer
		this.ExecCommand("docker run --name=server --network=none -d -p 8080:80 server-image");
	//connect vServer to vSwitch
		this.ExecCommand("ovs-docker add-port ovs-br1 eth0 server "
				+ "--ipaddress=192.168.95.233/24 "
				+ "--gateway=192.168.95.1 "
				+ "--macaddress=66:66:66:66:66:67 "
				+ "--mtu=1300");
		return 0;
	
	}
	
	public int VServerRelease()
	{
	//release VServer
		this.ExecCommand("docker stop server");
		this.ExecCommand("docker rm server");
		return 0;
	}
	
	
	
}
