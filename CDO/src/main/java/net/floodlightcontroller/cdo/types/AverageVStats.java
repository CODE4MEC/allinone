package net.floodlightcontroller.cdo.types;

import java.io.IOException;
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
	
	
	public AverageVStats()
	{
		;
	}
	
	public AverageVStats(String nid, float average_cpu_percent, float average_mem_percent)
	{	
		this.m_id_mecNode=nid;
		this.m_cpu_percent=average_cpu_percent;
		this.m_mem_percent=average_mem_percent;
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


