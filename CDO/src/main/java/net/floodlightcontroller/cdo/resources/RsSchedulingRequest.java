package net.floodlightcontroller.cdo.resources;

import java.io.IOException;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.floodlightcontroller.cdo.CdoService;
import net.floodlightcontroller.cdo.types.Alarm;
import net.floodlightcontroller.cdo.types.UrlType;


public class RsSchedulingRequest extends ServerResource {

	@Post("json")
    public String post(String msg_alarm) {
	  String resp= UrlType.RESP_FAIL;  
	  CdoService h_cdo = (CdoService) getContext()  
              .getAttributes().get(CdoService.class.getCanonicalName());  
	  
	  // 
	  if(!h_cdo.GetScheduleFlag())
	  {
		  return UrlType.RESP_FAIL;
	  }
	  
	  //debug 
      System.out.println("[CDO] msg received:"+msg_alarm);
      
      
      Alarm alarm;
      
      
      try {
    	  	// 
        ObjectMapper mapper = new ObjectMapper();
		alarm = mapper.readValue(msg_alarm, Alarm.class);
		
	      
	     if(0==h_cdo.CollectingSchedulingRequests(alarm))
	     {
	    	 	resp="success";	
	     }
	     
	     
      } catch (JsonParseException e) {
    	  	// TODO Auto-generated catch block
    	  	e.printStackTrace();
      } catch (JsonMappingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
      } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
      }
      
      
      return resp;
    }
}
