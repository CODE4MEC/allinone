package net.floodlightcontroller.cdo.resources;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.cdo.CdoService;
import net.floodlightcontroller.cdo.types.NormalMessage;
import net.floodlightcontroller.cdo.types.UrlType;

public class RsReleaseRequest extends ServerResource {

	@Post("json")
    public String post(String msg_code_release) {
		
	  CdoService h_cdo = (CdoService) getContext()  
              .getAttributes().get(CdoService.class.getCanonicalName()); 
	  //return UrlType.RESP_UNKNOWN;
	  
      System.out.println("[CDO] code_release_req received:"+msg_code_release);
      NormalMessage req_coderelease=NormalMessage.FromJsonString(msg_code_release);
      if(req_coderelease==null)
      {
    	  System.out.println("[CDO] cannot parse code_release_req:"+msg_code_release);
    	  return UrlType.RESP_FAIL;
      }
      
      if(req_coderelease.GetMsgType().equals(NormalMessage.CODE_RELEASE_P1_CHECK_PERMISSION))
      {
    	  // param1:global_hvipsname(e.g. Node1_vIPS0)
    	  String global_hvipsname=req_coderelease.GetParam1();
    	  if( h_cdo.CheckPermissionNReleasePort(global_hvipsname))
    	  {
    		  
    		  return UrlType.RESP_SUCCESS;
    	  }
    	  else
    	  {
    		  return UrlType.RESP_FAIL;
    	  }
      }
      else if(req_coderelease.GetMsgType().equals(NormalMessage.CODE_RELEASE_P2_REQ_DEL_RULES))
      {
    	  //TD:  
    	// param1:global_hvipsname(e.g. Node1_vIPS0)
    	  String global_hvipsname=req_coderelease.GetParam1();
    	  int ret=h_cdo.ReleaseCodeRulesNRecords(global_hvipsname);
    	  if(ret==0)
    	  {
//    		  System.out.println("[cdo] succesfully released CODE PAIR:");
    		  return UrlType.RESP_SUCCESS;
    	  }
    	  else
    	  {
    		  return UrlType.RESP_FAIL;
    	  }
    	  
      }
      else 
      {
    	  return UrlType.RESP_UNKNOWN;
      }
      
    }
}
