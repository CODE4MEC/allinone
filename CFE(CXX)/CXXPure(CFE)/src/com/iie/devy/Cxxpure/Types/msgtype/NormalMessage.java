package com.iie.devy.Cxxpure.Types.msgtype;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class NormalMessage {
	@JsonProperty("MSG_TYPE")
	protected String msg_type;
	
	@JsonProperty("MSG_PARAM1")
	protected String msg_param1="";
	
	@JsonProperty("MSG_PARAM2")
	protected String msg_param2="";
	
	@JsonIgnore
	public static final String REQ_NEW_HELPING_VIPS="REQ_NEW_HELPING_VIPS";
	@JsonIgnore
	public static final String RSP_NEW_HELPING_VIPS="RSP_NEW_HELPING_VIPS";
	@JsonIgnore
	public static final String REQ_AVAIL_UFD_VIPS_PORT="REQ_AVAIL_UFD_VIPS_PORT";
	@JsonIgnore
	public static final String RSP_AVAIL_UFD_VIPS_PORT="RSP_AVAIL_UFD_VIPS_PORT";
	@JsonIgnore
	public static final String REQ_OVS_COMMAND="REQ_OVS_COMMAND";
	@JsonIgnore
	public static final String CODE_RELEASE_P1_CHECK_PERMISSION="CODE_RELEASE_P1_CHECK_PERMISSION";
	@JsonIgnore
	public static final String CODE_RELEASE_P2_REQ_DEL_RULES="CODE_RELEASE_P2_REQ_DEL_RULES";
	@JsonIgnore
	public static final String REQ_RELEASE_REQUESTERS_UFD_OVS_PORT="REQ_RELEASE_REQUESTERS_UFD_OVS_PORT";
	
	public NormalMessage()
	{
		;
	}
	
	public NormalMessage(String msgtype)
	{
		this.msg_type=msgtype;
	}
	
	public NormalMessage(String msgtype,String msgparam1) {
		this.msg_type=msgtype;
		this.msg_param1=msgparam1;
	}
	
	public NormalMessage(String msgtype, String msgparam1, String msgparam2)
	{
		this.msg_type=msgtype;
		this.msg_param1=msgparam1;
		this.msg_param2=msgparam2;
	}
	
	
	@JsonIgnore
	public String GetMsgType()
	{
		return this.msg_type;
	}
	
	@JsonIgnore
	public String GetParam1()
	{
		return this.msg_param1;
	}
	
	@JsonIgnore
	public String GetParam2()
	{
		return this.msg_param2;
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
	public static NormalMessage FromJsonString(String jsonStr)
	{
		try {
    	  	//解析msg
        ObjectMapper mapper = new ObjectMapper();
        NormalMessage normalmsg = mapper.readValue(jsonStr, NormalMessage.class);
		return normalmsg;	     
      } catch (IOException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } 
	 
		return null;	
	}

}
