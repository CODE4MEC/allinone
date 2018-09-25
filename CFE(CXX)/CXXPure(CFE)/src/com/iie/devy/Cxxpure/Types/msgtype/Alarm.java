package com.iie.devy.Cxxpure.Types.msgtype;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Alarm {
	
	@JsonProperty("MSG_TYPE")
	private final String MSG_TYPE="TYPE_ALARM";
	
	@JsonProperty("NID")
	private String m_id_mecNode;
	
	
	public Alarm()
	{
		
	}
	
	public Alarm(String id_node)
	{
		this.m_id_mecNode=id_node;
	}
	
	@JsonIgnore
	public String GetNid()
	{
		return this.m_id_mecNode;
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
	public static Alarm FromJsonString(String jsonStr)
	{
		try {
    	  	//解析msg
        ObjectMapper mapper = new ObjectMapper();
        Alarm obj = mapper.readValue(jsonStr, Alarm.class);
		return obj;	     
      } catch (IOException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } 
	 
		return null;	
	}
}
