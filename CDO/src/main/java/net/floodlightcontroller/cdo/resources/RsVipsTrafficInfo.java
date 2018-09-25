package net.floodlightcontroller.cdo.resources;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.cdo.CdoService;
import net.floodlightcontroller.cdo.types.UrlType;
import net.floodlightcontroller.cdo.types.VipsTrafficInfo;

public class RsVipsTrafficInfo extends ServerResource {

	public RsVipsTrafficInfo() {
		// TODO Auto-generated constructor stub
	}
	
	@Post("json")  
	 public String post(String msg_vips_traffic_info)  {
		  //System.out.println("[msg received]"+msg_vips_traffic_info);
		
		  CdoService cdoservice = (CdoService) getContext()  
		          .getAttributes().get(CdoService.class.getCanonicalName());
		  VipsTrafficInfo vt = VipsTrafficInfo.FromJsonString(msg_vips_traffic_info);
		  
		  if(vt == null)
		  {
			  return UrlType.RESP_FAIL;
		  }
		  
		  if(0==cdoservice.UpdateVipsTrafficInfo(vt))
		  {
			  return UrlType.RESP_SUCCESS;
		  }
		  else
		  {
			  return UrlType.RESP_FAIL;
		  }
      
	} 

}
