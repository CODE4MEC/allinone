package com.iie.devy.Cxxpure.Types.msgtype;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UfdOffload {

	//the UFD port connected to the overloaded vIPS
	@JsonProperty("port")
	private int overload_port;
	
//	@JsonProperty("AVAIL_PORT")
	@JsonIgnore
//	private int avail_port;
	
	public UfdOffload()
	{ 
		;
	}
	
	public UfdOffload(int olPort) {
		this.overload_port=olPort;
//		this.avail_port=avPort;
	}
	
	@JsonIgnore
	public void SetOverloadPort(int olPort)
	{
		this.overload_port=olPort;
	}
	
//	@JsonIgnore
//	public void SetAvailPort(int avPort)
//	{
//		this.avail_port=avPort;
//	}
	
	@JsonIgnore
	public int GetOverloadPort()
	{
		return this.overload_port;
	}
	
//	@JsonIgnore
//	public int GetAvailPort()
//	{
//		return this.avail_port;
//	}
	
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
	public static UfdOffload FromJsonString(String jsonStr)
	{
		try {
			//parse message
        ObjectMapper mapper = new ObjectMapper();
        UfdOffload obj = mapper.readValue(jsonStr, UfdOffload.class);
		return obj;	     
      } catch (IOException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } 
	 
		return null;	
	}

}
