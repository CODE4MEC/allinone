package com.iie.devy.Cxxpure.Types.msgtype;

import java.io.IOException;
import java.util.Hashtable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VipsTrafficInfo {

	@JsonProperty("INFO")
	private Hashtable<String,Long> ht_vips_traffics=new Hashtable <String,Long>();
	
	public VipsTrafficInfo()
	{
		;
	}
	
	public VipsTrafficInfo(Hashtable<String,Long> trafficinfo) {
		this.ht_vips_traffics=trafficinfo;
	}
	
	@JsonIgnore 
	public Hashtable<String, Long> GetInfo()
	{
		return this.ht_vips_traffics;
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
	public static VipsTrafficInfo FromJsonString(String jsonStr)
	{
		try {
			//parse message
        ObjectMapper mapper = new ObjectMapper();
        VipsTrafficInfo obj = mapper.readValue(jsonStr, VipsTrafficInfo.class);
		return obj;	     
      } catch (IOException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } 
	 
		return null;	
	}

}
