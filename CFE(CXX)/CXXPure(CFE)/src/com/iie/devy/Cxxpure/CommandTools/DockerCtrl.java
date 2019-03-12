/* vIPS(docker) Control Tools*/


package com.iie.devy.Cxxpure.CommandTools;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
	
	
	public int InitLocalWorkingVIPS(String ID)
	{
		int ret=0;
		
		//Connect vIPS to UFD
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
		
		//connect vIPS to vSwitch (not UFD!!)
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
		
		//release if failed
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
	
	//release local working vIPS, working vIPS -> reserved vIPS
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
		
		//the 0th line is abandoned
		for(int i=1;i<tokens.length;i++)
		{
			ContainerStats stats=new ContainerStats(tokens[i],id_mecNode);
			statsmap.put(stats.getName(), stats);
		}
		return statsmap;
	}	
	
	//dev19 
		public String  GetContextInfoFromVIPS(String vips_name, String SIPs)
		{
//			String command="docker exec -i "+vips_name+" /bin/sh /root/getcontext.sh -s "+" \""+SIPs+"\"";
//			return this.ExecCommand(command);
			final String filename="./SIP.txt";
			WriteFile(filename,SIPs);
			String command="docker cp "+filename+" "+vips_name+":/root/";
			this.ExecCommand(command);
			command="docker exec -i "+vips_name+" /bin/sh /root/getcontext.sh";
			return this.ExecCommand(command);
		}
		public int SetContextInfoToVIPS(String vips_name,String contextinfo)
		{
//			String command="docker exec -i "+vips_name+" /bin/sh /root/setcontext.sh -c "+"\""+contextinfo+"\"";
			final String filename="./contextinfo.txt";
			WriteFile(filename,contextinfo);
			String command="docker cp "+filename+" "+vips_name+":/root/";
			this.ExecCommand(command);
			command="docker exec -i "+vips_name+" /bin/sh /root/setcontext.sh";
			String ret= this.ExecCommand(command);
			System.out.println(ret);
			if(ret.contains("DySuccess"))
			{
				return 0;
			}
			else 
			{
				logger.error(ret);
				return -1;
			}
		}
		
		private void WriteFile(String name, String content)
		{
			try {
				FileOutputStream outSTr;
				outSTr = new FileOutputStream(new File(name));
				BufferedOutputStream Buff = new BufferedOutputStream(outSTr);
	            Buff.write(content.getBytes());
	            Buff.flush();
	            Buff.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
		}
		//edev19
}
