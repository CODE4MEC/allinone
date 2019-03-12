/*
 * The thread for monitoring the information of UFD
 */
package com.iie.devy.Threads;

import org.apache.log4j.Logger;

import com.iie.devy.Cxxpure.CXX;
import com.iie.devy.Cxxpure.Default.ParamDefault;
import com.iie.devy.Cxxpure.Types.msgtype.VipsTrafficInfo;

public class TdUStatListen extends Thread {
	private static final Logger logger=Logger.getLogger(TdUStatListen.class);
	
	private CXX m_cxx;
	private boolean stopflag=false;
	public TdUStatListen(CXX cxx) {
		this.m_cxx=cxx;
	}
	
	public void Shutdown()
	{
		stopflag=true;
	}
	
	
	@Override
	public void run() {
		try {
			logger.info("TdUStatListen on");
			while(!this.stopflag)
			{
				String rawinfo=this.m_cxx.GetUfdPortTrafficInfo();
				VipsTrafficInfo info=this.m_cxx.GetVipsTrafficInfoFromRawString(rawinfo);
				if(info.GetInfo().size()>0)
				{
					this.m_cxx.ReportVipsTrafficInfo(info);
				}
				Thread.sleep(ParamDefault.SAMPLE_PERIOD_TYPE_CT);
			}
			logger.info("TdUStatListen stop");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

}
