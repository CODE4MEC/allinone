package com.iie.devy.Cxxpure.CommandTools;


import java.util.Hashtable;

import org.apache.log4j.Logger;

import com.iie.devy.Cxxpure.Types.msgtype.ContainerStats;

public class DockerCtrl extends CommandExecution {
	private static final Logger logger=Logger.getLogger(DockerCtrl.class);
	public DockerCtrl()
	{
		super();
	}
	public Runtime getRuntime()
	{
		return run;
	}
	
	//TD:对于调用该方法的方法，记得检查返回值，若返回值不为0，则释放对应ID资源
	public int InitLocalWorkingVIPS(String ID)
	{
		int ret=0;
		
		//将VIPS连接到UFD
		String str_ret=this.ExecCommand("ovs-docker add-port ovs-br2 eth0 "
					+ ID +" "
					+"--ipaddress=192.168.97.101/24 "
					+"--macaddress=66:66:66:66:66:66 "
					+"--gateway=192.168.97.1");
		if(!str_ret.contains("Error"))
		{
			logger.info("Sucessfully init a new VIPS from reserved, name="+ID);
		}
		else
		{
			logger.warn("Failed to init a new VIPS from reserved, name="+ID);
			ret=-1;
		}
					
		return ret;
	}
	
	public int InitHelpingWorkingVIPS(String ID)
	{
		int ret=0;
		
		//将vIPS连接到OVS（不是UFD!）
		String str_ret=this.ExecCommand("ovs-docker add-port ovs-br1 eth0 "
				+ ID +" "
				+"--ipaddress=192.168.97.101/24 "
				+"--macaddress=66:66:66:66:66:66 "
				+"--gateway=192.168.97.1");
		
		if(!str_ret.contains("Error"))
		{
			logger.info("[info] Sucessfully init a new VIPS from reserved, name="+ID);
		}
		else
		{
			logger.warn("[info] Failed to init a new VIPS from reserved, name="+ID);
			ret=-1;
		}
		
		return ret;
	}
	
	public int StartReservedVIPS(String ID, double paramCpus) 
	{
			 
		String answer="";
		answer = ExecCommand("docker run -d" + 
				" --name="+ID + 
				" --network=none " + 
				" --privileged " + 
				" --cap-add NET_ADMIN " + 
				" --cap-add NET_RAW " + 
				" --cpus=" + paramCpus + " "+
				//" --cpuset-cpus="+ paramCpuset +" "+
				" ips-image");
		
		//若创建失败，则释放
		if(answer.isEmpty() || answer.contains("Error"))
		{
			logger.error("error in StartVIPS, Fail to Start, answer="+answer);
			String command;
			command="docker stop"+" "+ ID;
			answer=ExecCommand(command);
			command = "docker rm" + " " + ID;
			answer=ExecCommand(command);
			
			
			return -3;
		}
		
		//debug 
		logger.info("Start a Reserved vIPS, name="+ID+", ret="+answer);
		return 0;
	}
	
	//将working vips转变为Reserved VIPS
	public int ReleaseLocalWorkingVIPS(String ID)
	{
		//DEL PORT
		ExecCommandWithSudo("ovs-docker del-port ovs-br2 eth0 "+ID);
		
		logger.info("Working vIPS:"+ID+" is released");
		return 0;
	}
	
	public int ReleaseHelpingWorkingVIPS(String ID)
	{
		//DEL PORT
		ExecCommandWithSudo("ovs-docker del-port ovs-br1 eth0 "+ID);
		
		logger.info("Working vIPS:"+ID+" is released");
		return 0;
	}
	
	public int ReleaseReservedVIPS(String ID)
	{
		//STOP
		StopVIPS(ID);
		//RM
		RemoveVIPS(ID);
		logger.info("Reserved vIPS:"+ID+" is released");
		return 0;
	}
	
	private int StopVIPS(String ID) 
	{
		String command = "docker stop"+" "+ ID;
		String answer="";
		answer=ExecCommand(command);	
		//debug 
		logger.info("Stop a VIPS, name="+ID+", ret="+answer);
		return 0;
	}
	
	public int RemoveVIPS(String ID) 
	{
		String command = "docker rm"+" "+ ID;
		String answer="";
		answer=ExecCommand(command);	
		//debug 
		logger.info("Remove a VIPS, name="+ID+", ret="+answer);
		return 0;
	}
	
	public Hashtable<String,ContainerStats> StatContainer(String id_mecNode) 
	{
		String command = "docker stats --no-stream";
		String answer="";
		answer=ExecCommand(command);	
		
		return ParseCStatsFromRaw(id_mecNode,answer);
	}
	
	public Hashtable<String,ContainerStats> ParseCStatsFromRaw(String id_mecNode,String raw_container_stats)
	{
		Hashtable<String,ContainerStats> statsmap=new Hashtable<String, ContainerStats>();
		String[] tokens=raw_container_stats.split("\n");
		
		//i从1开始（不要第0行）
		for(int i=1;i<tokens.length;i++)
		{
			ContainerStats stats=new ContainerStats(tokens[i],id_mecNode);
			statsmap.put(stats.getName(), stats);
		}
		return statsmap;
	}	
}
