package net.floodlightcontroller.cdo.resources;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.cdo.CdoService;
import net.floodlightcontroller.cdo.types.NodeStats;
import net.floodlightcontroller.cdo.types.UrlType;

public class RsNodeStats extends ServerResource {

	
	@Post("json")  
	 public String post(String msg_node_stats)  {
		  System.out.println("[msg received]"+msg_node_stats);
		
		  CdoService cdoservice = (CdoService) getContext()  
		          .getAttributes().get(CdoService.class.getCanonicalName());
		  NodeStats ns = NodeStats.FromJsonString(msg_node_stats);
		  
		  if(ns == null)
		  {
			  return UrlType.RESP_FAIL;
		  }
		  
		  if(0==cdoservice.UpdateNodeStats(ns))
		  {
			  return UrlType.RESP_SUCCESS;
		  }
		  else
		  {
			  return UrlType.RESP_FAIL;
		  }
       
	}  
}
