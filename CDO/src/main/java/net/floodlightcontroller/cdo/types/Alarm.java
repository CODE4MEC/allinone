package net.floodlightcontroller.cdo.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
}
