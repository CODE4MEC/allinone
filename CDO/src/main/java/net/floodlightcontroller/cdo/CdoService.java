package net.floodlightcontroller.cdo;

import java.util.List;

import net.floodlightcontroller.cdo.types.Alarm;
import net.floodlightcontroller.cdo.types.AverageVStats;
import net.floodlightcontroller.cdo.types.ContainerStats;
import net.floodlightcontroller.cdo.types.NodeOnOffState;
import net.floodlightcontroller.cdo.types.NodeStats;
import net.floodlightcontroller.cdo.types.VipsTrafficInfo;
import net.floodlightcontroller.core.module.IFloodlightService;

public interface CdoService extends IFloodlightService {
	static boolean m_f_stop=false;
	
	String Test();
	int UpdateStats(List<ContainerStats> l);
	
	// 
	int CollectingSchedulingRequests(Alarm alarm);
	int UpdateOnOffState(NodeOnOffState onoff_state);	
	
	// 
	int UpdateNodeStats(NodeStats stats);
	
	boolean CheckPermissionNReleasePort(String global_vips_id);
	int ReleaseCodeRulesNRecords(String global_hvipsname);
	
	
	int UpdateAverageVipsStats(AverageVStats avs);
	int UpdateVipsTrafficInfo(VipsTrafficInfo vt);
	
	boolean GetScheduleFlag();
	void DisableSchedule();
	int EnableSchedule() throws InterruptedException;
	
	
	
	
}
