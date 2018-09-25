package com.iie.devy.Cxxpure.Types.msgtype;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VipsStats extends ContainerStats {

	public VipsStats(String nid, String cid, String name, float cpu_percent, float mem_percent) {
		super(nid, cid, name, cpu_percent, mem_percent);
		// TODO Auto-generated constructor stub
	}

	public VipsStats(String info, String id_mecNode) {
		super(info, id_mecNode);
		// TODO Auto-generated constructor stub
	}
	
	public VipsStats(ContainerStats cs,double param_cpu_nums)
	{
		super(cs);
		RectifyCpuPercent(param_cpu_nums);
		CheckAlarm();
	}
	
	//修正cpu占比
	public void RectifyCpuPercent(double param_cpu_nums)
	{
		if(0!=param_cpu_nums)
		{
			this.m_cpu_percent /= param_cpu_nums;
		}	
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
	public static VipsStats FromJsonString(String jsonStr)
	{
		try {
    	  	//解析msg
        ObjectMapper mapper = new ObjectMapper();
        VipsStats obj = mapper.readValue(jsonStr, VipsStats.class);
		return obj;	     
      } catch (IOException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } 
	 
		return null;	
	}
}
