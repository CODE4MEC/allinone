package net.floodlightcontroller.cdo.resources;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.cdo.CdoService;
import net.floodlightcontroller.cdo.types.ScheduleFlag;
import net.floodlightcontroller.cdo.types.UrlType;

public class RsSetSchedule extends ServerResource {

	public RsSetSchedule() {
		// TODO Auto-generated constructor stub
	}
	
	@Post("json")
    public String post(String msg) throws InterruptedException {
		CdoService h_cdo = (CdoService) getContext()  
	              .getAttributes().get(CdoService.class.getCanonicalName()); 
		ScheduleFlag flag = ScheduleFlag.FromJsonString(msg);
		if(flag==null)
		{
			return UrlType.RESP_UNKNOWN;
		}
		
		if(flag.GetFlag())
		{
			h_cdo.EnableSchedule();
			System.out.println("[info] Set ScheduleFlag:"+h_cdo.GetScheduleFlag());
			return "ScheduleFlag status:"+h_cdo.GetScheduleFlag();
		}
		else
		{
			h_cdo.DisableSchedule();
			System.out.println("[info] Set ScheduleFlag:"+h_cdo.GetScheduleFlag());
			return "ScheduleFlag status:"+h_cdo.GetScheduleFlag(); 
		}
	}

}
