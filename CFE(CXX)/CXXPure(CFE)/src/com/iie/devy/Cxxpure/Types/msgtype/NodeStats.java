package com.iie.devy.Cxxpure.Types.msgtype;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iie.devy.Cxxpure.CXX;

public class NodeStats {
	@JsonProperty("MSG_TYPE")
	protected final String MSG_TYPE="TYPE_STATS_NODE";
	@JsonProperty("NID")
	protected String m_id_mecNode;
	
	@JsonProperty("OVS_MAC")
	private String m_MAC_OVS;
	
	@JsonProperty("CNUM_ALL")
	protected Integer m_CNum_all;
	
	@JsonProperty("CNUM_TAKEN")
	protected Integer m_CNum_taken;
	
	public NodeStats()
	{
		
	}
	
	public NodeStats(String id_node, String str_mac_ovs, int container_num_all, int container_num_taken)
	{
		this.m_id_mecNode=id_node;
		this.m_MAC_OVS=str_mac_ovs;
		this.m_CNum_all=container_num_all;
		this.m_CNum_taken=container_num_taken;
	}
	
	public NodeStats(CXX cxx) {
		this.m_id_mecNode=cxx.GetMecNodeID();
		this.m_MAC_OVS=cxx.GetOvsMacAddr();
		this.m_CNum_all=cxx.GetMaxContainerNum();
		this.m_CNum_taken=cxx.GetRunningContainerNum();
	}

	@JsonIgnore 
	public String GetNID()
	{
		return this.m_id_mecNode;
	}
	
	@JsonIgnore 
	public int GetMaxCNum()
	{
		return this.m_CNum_all;
	}
	
	@JsonIgnore 
	public int GetUsedCNum()
	{
		return this.m_CNum_taken;
	}
	
	
	@JsonIgnore 
	public int GetAvailCNum()
	{
		return this.m_CNum_all-this.m_CNum_taken;
	}
	
	@JsonIgnore
	public String GetOvsMacAddr()
	{
		return this.m_MAC_OVS;
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
	public static NodeStats FromJsonString(String jsonStr)
	{
		try {
    	  	//解析msg
        ObjectMapper mapper = new ObjectMapper();
        NodeStats ns = mapper.readValue(jsonStr, NodeStats.class);
		return ns;	     
      } catch (IOException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } 
	 
		return null;	
	}
	
}
