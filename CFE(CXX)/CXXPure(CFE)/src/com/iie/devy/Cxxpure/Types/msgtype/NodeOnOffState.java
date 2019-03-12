package com.iie.devy.Cxxpure.Types.msgtype;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NodeOnOffState {
	public static final int STATE_ON=0;
	public static final int STATE_OFF=1;
	
	@JsonProperty("NID")
	private String m_id_mecNode;
	
	@JsonProperty("OVS_MAC")
	private String m_mac_ovs;
	
	@JsonProperty("IP_NODE")
	private String m_ip_node;
	
	@JsonProperty("IP_UP")
	private String m_ip_up;//user plane
	
	@JsonProperty("IP_VSERVER")
	private String m_ip_vserver;
	
	@JsonProperty("PORT_OVS_VSERVER")
	private int m_port_ovs_vserver;
	
	@JsonProperty("NSTATE")
	private int m_state_onoff;
	
	public NodeOnOffState()
	{
		;
	}
	
	public NodeOnOffState(String id_node, String str_mac_ovs, String ip_node, String ip_up, String ip_vserver, int port_ovs_vserver, int state_onoff)
	{
		m_mac_ovs=str_mac_ovs;
		m_id_mecNode=id_node;
		m_state_onoff=state_onoff;
		m_ip_node=ip_node;
		m_ip_up=ip_up;
		m_ip_vserver=ip_vserver;
		m_port_ovs_vserver=port_ovs_vserver;
	}
	
	@JsonIgnore
	public String GetNid()
	{
		return this.m_id_mecNode;
	}
	
	@JsonIgnore
	public int GetOnOffState()
	{
		return m_state_onoff;
	}
	
	@JsonIgnore
	public String GetOvsMacAddr()
	{
		return this.m_mac_ovs;
	}
	
	@JsonIgnore
	public String GetIpAddr()
	{
		return this.m_ip_node;
	}
	
	@JsonIgnore
	public String GetUserPlaneIp()
	{
		return this.m_ip_up;
	}
	
	@JsonIgnore
	public String GetVserverIp()
	{
		return this.m_ip_vserver;
	}
	
	@JsonIgnore
	public int GetOvsVserverPort()
	{
		return this.m_port_ovs_vserver;
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
	public static NodeOnOffState FromJsonString(String jsonStr)
	{
		try {
			//parse message
        ObjectMapper mapper = new ObjectMapper();
        NodeOnOffState obj = mapper.readValue(jsonStr, NodeOnOffState.class);
		return obj;	     
      } catch (IOException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } 
	 
		return null;	
	}
}

