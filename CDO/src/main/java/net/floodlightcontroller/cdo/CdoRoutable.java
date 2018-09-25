package net.floodlightcontroller.cdo;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.cdo.resources.*;
import net.floodlightcontroller.cdo.types.UrlType;
import net.floodlightcontroller.restserver.RestletRoutable;

public class CdoRoutable implements RestletRoutable {

	@Override
    public Restlet getRestlet(Context context) {
		
        Router router = new Router(context);
        
        router.attach(UrlType.SUFFIX_CDO_ONOFF_STATS,NodeInform.class);     
        router.attach(UrlType.SUFFIX_CDO_CODE_ESTABLISH,RsSchedulingRequest.class);       
        router.attach(UrlType.SUFFIX_CDO_LINK_STATS,RsLinkStats.class); 
        router.attach(UrlType.SUFFIX_CDO_NODE_STATS,RsNodeStats.class); 
        router.attach(UrlType.SUFFIX_CDO_CODE_RELEASE,RsReleaseRequest.class); 
        router.attach(UrlType.SUFFIX_CDO_TEST,Test.class);
        router.attach(UrlType.SUFFIX_CDO_AVERAGE_VIPS_STATS,RsAverageVipsStats.class);
        router.attach(UrlType.SUFFIX_CDO_SET_SCHEDULE,RsSetSchedule.class);
        router.attach(UrlType.SUFFIX_CDO_VIPS_TRAFFIC,RsVipsTrafficInfo.class);

        
//        router.attach("/onoff_inform/json",NodeInform.class);         
//        router.attach("/code_request/json",RsSchedulingRequest.class);     
//        router.attach("/link_stats/json",RsLinkStats.class); 
//        router.attach("/node_stats/json",RsNodeStats.class); 
//        //router.attach("/container_stats/json",RsContainerStats.class); 
//        router.attach("/release_request/json",RsReleaseRequest.class); 

        return router;
    }

    @Override
    public String basePath() {
        return "";
    }
}
