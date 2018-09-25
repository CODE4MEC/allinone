package net.floodlightcontroller.cdo.resources;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.cdo.CdoService;
import net.floodlightcontroller.cdo.types.NodeOnOffState;
import net.floodlightcontroller.cdo.types.UrlType;

//  
public class NodeInform extends ServerResource {

	@Post("json")
    public String post(String msg_onoff_state) { 
	  //debug 
      System.out.println("[CDO] msg received:"+msg_onoff_state);
      
      
      NodeOnOffState onoff_state=NodeOnOffState.FromJsonString(msg_onoff_state);
      if(onoff_state==null)
      {
    	  return UrlType.RESP_FAIL;
      }
      
      CdoService h_cdo = (CdoService) getContext()  
              .getAttributes().get(CdoService.class.getCanonicalName()); 
      
      if(0==h_cdo.UpdateOnOffState(onoff_state))
	  {
    	  return UrlType.RESP_SUCCESS;
	  }
      else
      {
    	  return UrlType.RESP_FAIL;
      }
    }
	
}
