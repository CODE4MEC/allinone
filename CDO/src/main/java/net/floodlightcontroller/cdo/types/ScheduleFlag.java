package net.floodlightcontroller.cdo.types;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ScheduleFlag {
	@JsonProperty("SCHEDULE_FLAG")
	private boolean schedule_flag;

	public ScheduleFlag() {
		// TODO Auto-generated constructor stub
	}
	
	public ScheduleFlag(boolean flag)
	{
		this.schedule_flag=flag;
	}
	
	@JsonIgnore
	public boolean GetFlag()
	{
		return this.schedule_flag;
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
	public static ScheduleFlag FromJsonString(String jsonStr)
	{
		try {
    	// 
        ObjectMapper mapper = new ObjectMapper();
        ScheduleFlag obj = mapper.readValue(jsonStr, ScheduleFlag.class);
		return obj;	     
      } catch (IOException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } 
	 
		return null;	
	}

}
