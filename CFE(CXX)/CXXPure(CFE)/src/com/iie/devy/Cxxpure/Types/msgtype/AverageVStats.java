package com.iie.devy.Cxxpure.Types.msgtype;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AverageVStats {
		
	@JsonProperty("NID")
	protected String m_id_mecNode;
	
	@JsonProperty("AVERAGE_CPU_PERCENT")
	protected float m_cpu_percent; // CPU %
	
	@JsonProperty("AVERAGE_MEM_PERCENT")
	protected float m_mem_percent; //MiB
	
	
	public AverageVStats(String nid, float average_cpu_percent, float average_mem_percent)
	{	
		this.m_id_mecNode=nid;
		this.m_cpu_percent=average_cpu_percent;
		this.m_mem_percent=average_mem_percent;
	}
	
	public AverageVStats(String nid,Hashtable<String,VipsStats> ht_av_cstats)
	{
		
		float tot_cpu=0;
		float tot_mem=0;
		int count=ht_av_cstats.size();
			
		if(count==0)
		{
			this.m_id_mecNode=nid;
			this.m_cpu_percent=0;
			this.m_mem_percent=0;
		}
		else
		{
			for(Entry<String, VipsStats> entry:ht_av_cstats.entrySet())
			{
				tot_cpu += entry.getValue().getCPUPercent();
				tot_mem += entry.getValue().getMemPercent();
			}
			this.m_id_mecNode=nid;
			this.m_cpu_percent = tot_cpu/count;
			this.m_mem_percent = tot_mem/count;
		}
		

	}
	
	public AverageVStats(AverageVStats acs)
	{
		this.m_id_mecNode=acs.getMecNodeID();
		this.m_cpu_percent=acs.getAverageCPUPercent();
		this.m_mem_percent=acs.getAverageMemPercent();
	}
	
	@JsonIgnore
	public String getMecNodeID()
	{
		return this.m_id_mecNode;
	}
	
	
	@JsonIgnore
	public float getAverageCPUPercent()
	{
		return this.m_cpu_percent;
	}
	
	@JsonIgnore
	public float getAverageMemPercent()
	{
		return this.m_mem_percent;
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
	public static AverageVStats FromJsonString(String jsonStr)
	{
		try {
			//parse message
        ObjectMapper mapper = new ObjectMapper();
        AverageVStats obj = mapper.readValue(jsonStr, AverageVStats.class);
		return obj;	     
      } catch (IOException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } 
	 
		return null;	
	}
	
}

