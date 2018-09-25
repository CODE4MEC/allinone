package net.floodlightcontroller.cdo.resources;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.cdo.CdoService;
import net.floodlightcontroller.cdo.types.AverageVStats;
import net.floodlightcontroller.cdo.types.UrlType;

public class RsAverageVipsStats extends ServerResource {

	@Post("json")  
	 public String post(String msg_avs)  {
		  System.out.println("[msg received]"+msg_avs);
		
		  CdoService cdoservice = (CdoService) getContext()  
		          .getAttributes().get(CdoService.class.getCanonicalName());
		  AverageVStats avs = AverageVStats.FromJsonString(msg_avs);
		  
		  if(avs == null)
		  {
			  return UrlType.RESP_FAIL;
		  }
		  
		  if(0==cdoservice.UpdateAverageVipsStats(avs))
		  {
			  return UrlType.RESP_SUCCESS;
		  }
		  else
		  {
			  return UrlType.RESP_FAIL;
		  }
      
 }  
}
