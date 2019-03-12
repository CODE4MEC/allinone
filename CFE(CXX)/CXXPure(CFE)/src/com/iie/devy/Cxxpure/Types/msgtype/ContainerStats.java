
package com.iie.devy.Cxxpure.Types.msgtype;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iie.devy.Cxxpure.Default.ParamDefault;

//example:
//CONTAINER ID        NAME                CPU %               MEM USAGE / LIMIT     MEM %               NET I/O             BLOCK I/O           PIDS
//ee10213a41fe        runoob              0.02%               11.54MiB / 1.952GiB   0.58%               1.04kB / 0B         0B / 0B             1

public class ContainerStats {
	
	protected String m_info;
	
	@JsonProperty("MSG_TYPE")
	protected final String MSG_TYPE="TYPE_STATS_CONTAINER";
	
	@JsonProperty("NID")
	protected String m_id_mecNode;
	
	@JsonProperty("CID")
	protected String m_id_container;
	
	@JsonProperty("NAME")
	protected String m_name;
	
	@JsonProperty("CPU_PERCENT")
	protected float m_cpu_percent; // CPU %
	
	@JsonProperty("MEM_PERCENT")
	protected float m_mem_percent; //MiB
	
//	private boolean m_f_cpu_expire=false;
//	private boolean m_f_mem_expire=false;
	
	//@JsonProperty("F_ALARM")
	protected boolean m_f_alarm=false;
	
	public ContainerStats(String nid,String cid, String name, float cpu_percent, float mem_percent)
	{
		this.m_info="";
		this.m_id_container=cid;
		this.m_id_mecNode=nid;
		this.m_name=name;
		this.m_cpu_percent=cpu_percent;
		this.m_mem_percent=mem_percent;
		
		CheckAlarm();
	}
	
	public ContainerStats(String info, String id_mecNode)
	{
		this.m_id_mecNode=id_mecNode;
		this.m_info=info;
		Parse(info);
	}
	
	public ContainerStats(ContainerStats cs)
	{
		this.m_info=cs.getInfo();
		this.m_id_container=cs.getContainerID();
		this.m_id_mecNode=cs.getMecNodeID();
		this.m_name=cs.getName();
		this.m_cpu_percent=cs.getCPUPercent();
		this.m_mem_percent=cs.getMemPercent();
		CheckAlarm();
	}
	
	public String Parse(String info)
	{
		String tmp="";
		while(info.contains("  "))
		{
			info=info.replaceAll("  ", " ");
		}		
		info=info.replaceAll(" / ", " ");
		
		String[] tokens=info.split(" ");
		this.m_id_container=tokens[0];
		this.m_name=tokens[1];
		
		tmp=tokens[2];
		tmp=tmp.replaceAll("%", "");
		this.m_cpu_percent=Float.parseFloat(tmp);
		
		tmp=tokens[5];
		tmp=tmp.replaceAll("%", "");
		this.m_mem_percent=Float.parseFloat(tmp);
		
		CheckAlarm();
		
		
		return this.m_name;
	}
	
	protected void CheckAlarm()
	{
		//alarm pre-judgment
		//if( ( this.m_cpu_percent > ParamDefault.TH_CPU_ALARM ) && (this.m_mem_percent > ParamDefault.TH_MEM_ALARM))
		if( ( this.m_cpu_percent > ParamDefault.TH_CPU_ALARM ) )

		{
			this.m_f_alarm=true;
		}
		else
		{
			this.m_f_alarm=false;
		}
	}
	
	@JsonIgnore
	public String getInfo()
	{
		return this.m_info;
	}
	
	@JsonIgnore
	public String getContainerID()
	{
		return this.m_id_container;
	}
	
	@JsonIgnore
	public String getMecNodeID()
	{
		return this.m_id_mecNode;
	}
	
	@JsonIgnore
	public String getName()
	{
		return this.m_name;
	}
	
	@JsonIgnore
	public float getCPUPercent()
	{
		return this.m_cpu_percent;
	}
	
	@JsonIgnore
	public float getMemPercent()
	{
		return this.m_mem_percent;
	}
	
	@JsonIgnore
	public boolean getAlarmFlag()
	{
		return this.m_f_alarm;
	}
	
	@JsonIgnore
	public String ToJsonString()
	{
		String jsonStr="";
	    try {
	    	ObjectMapper mapper = new ObjectMapper();
	    	jsonStr = mapper.writeValueAsString(this);
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return jsonStr;
	}
	
	@JsonIgnore
	public static ContainerStats FromJsonString(String jsonStr)
	{
		try {
			//parse message
        ObjectMapper mapper = new ObjectMapper();
        ContainerStats obj = mapper.readValue(jsonStr, ContainerStats.class);
		return obj;	     
      } catch (IOException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } 
	 
		return null;	
	}
	
}
