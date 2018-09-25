package net.floodlightcontroller.cdo.types;

public final class UrlType {

	// DB related
		public static final String URL_DB="http://localhost:8086";
		public static final String SUFFIX_DB_QUERY="/query";
		public static final String SUFFIX_DB_WRITE="/write";
	
	// CDO related	
		public static final int CDO_CODE_ESTABLISH=1;
		public static final int CDO_CODE_RELEASE=2;
		public static final int CDO_NODE_STATS=3;
		public static final int CDO_CONTAINER_STATS=4;
		public static final int CDO_LINK_STATS=5;
		public static final int CDO_ONOFF_STATS=6;
		public static final int CDO_AVERAGE_VIPS_STATS=7;
		
		public static final String SUFFIX_CDO_CODE_ESTABLISH="/wm/cdo/code_establish/json";
		public static final String SUFFIX_CDO_CODE_RELEASE="/wm/cdo/code_release/json";
		public static final String SUFFIX_CDO_NODE_STATS="/wm/cdo/node_stats/json";
		public static final String SUFFIX_CDO_CONTAINER_STATS="/wm/cdo/container_stats/json";
		public static final String SUFFIX_CDO_LINK_STATS="/wm/cdo/link_stats/json";
		public static final String SUFFIX_CDO_ONOFF_STATS="/wm/cdo/onoff_inform/json";
		public static final String SUFFIX_CDO_TEST="/wm/cdo/test";
		public static final String SUFFIX_CDO_AVERAGE_VIPS_STATS="/wm/cdo/avs";
		public static final String SUFFIX_CDO_SET_SCHEDULE="/wm/cdo/set_schedule/json";
		public static final String SUFFIX_CDO_VIPS_TRAFFIC="/wm/cdo/vips/traffic";
		
	// CXX related
//		public static final int CXX_NEW_CONTAINER=1;
//		public static final int CXX_RELEASE_CONTAINER=1;
		
		public static final String SUFFIX_CXX_CODE_PAIR_INIT="/code_pair_init";
		public static final String SUFFIX_CXX_CODE_PAIR_RELEASE="/code_pair_release";
		
		public static final int CXX_API_PORT=8002;
		public static final int CDO_API_PORT=8080;
		

		
		public static final String RESP_SUCCESS="success";
		public static final String RESP_FAIL="fail";
		public static final String RESP_UNKNOWN="unknown";
		
		//CDO unique
		//public static final String USER_IP_WITHMASK="192.168.96.0/24";
		public static final String SERVER_IP_WITHMASK="192.168.95.0/24";
		public static final String NODE_IP_USER="192.168.0.2";
		


	}
