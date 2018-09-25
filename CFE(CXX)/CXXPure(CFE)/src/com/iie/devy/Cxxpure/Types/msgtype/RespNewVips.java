package com.iie.devy.Cxxpure.Types.msgtype;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RespNewVips {

	@JsonProperty("PORT")
	protected int m_port;
	
	
	
	public RespNewVips(int port) {
		SetPort(port);
	}

	@JsonIgnore
	public int GetPort()
	{
		return this.m_port;
	}
	
	
	@JsonIgnore
	public void SetPort(int port)
	{
		this.m_port=port;
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
	public static RespNewVips FromJsonString(String jsonStr)
	{
		try {
    	  	//解析msg
			ObjectMapper mapper = new ObjectMapper();
			RespNewVips obj = mapper.readValue(jsonStr, RespNewVips.class);
			return obj;	     
      } catch (IOException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } 
	 
		return null;	
	}
}
