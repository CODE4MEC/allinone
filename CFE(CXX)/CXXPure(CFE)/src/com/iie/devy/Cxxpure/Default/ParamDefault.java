package com.iie.devy.Cxxpure.Default;

public class ParamDefault {

//FINAL
	public static final int SAMPLE_PERIOD_TYPE_IM = 1000;//vips status sampling period, used for TdStatListen typeIM
	public static final int SAMPLE_PERIOD_TYPE_CT = 1000;//vips status sampling period, used for TdStatListen typeCT
	
	public static final int MAX_FOREIGN_HELPING_VIPS=100;//maximum number of foreign helping vips
	
	//vips type
	public static final int TYPE_HELPING_VIPS=0;
	public static final int TYPE_LOCAL_VIPS=1;
	public static final int TYPE_UNKNOWN_VIPS=2;
	
	//ALARM RELATED
	public static final int TH_CPU_ALARM=80; //alarm threshold

	//public static final int TH_MEM_ALARM=0;
	public static final int TH_ALARM_PERIOD = 1000;
	public static final int TH_ALARM_SLOT_NUM_TYPE_IM=TH_ALARM_PERIOD/SAMPLE_PERIOD_TYPE_IM;//consecutive time slot for alarm triggering, used for TdStatListen typeIM
	public static final int TH_ALARM_SLOT_NUM_TYPE_CT=TH_ALARM_PERIOD/SAMPLE_PERIOD_TYPE_CT;//consecutive time slot for alarm triggering, used for TdStatListen typeCT	
	//RELEASE RELATED
	public static final int TH_CPU_RELEASE_LOCAL=70; //used for local vips release，SUM(Overhead)<(N-1)*TH_CPU_RELEASE_LOCAL
	public static final int TH_CPU_RELEASE_HELPING=20;//used for helpingvips release, Overhead<TH_CPU_RELEASE_HELPING
	//public static final int TH_MEM_RELEASE=0;
	public static final int TH_RELEASE_PERIOD = 15000;
	public static final int TH_RELEASE_SLOT_NUM_TYPE_IM = TH_RELEASE_PERIOD/SAMPLE_PERIOD_TYPE_IM;//releasing threshold, used for TdStatListen typeIM
	public static final int TH_RELEASE_SLOT_NUM_TYPE_CT = TH_RELEASE_PERIOD/SAMPLE_PERIOD_TYPE_CT;//releasing threshold, used for TdStatListen typeCT
	//ADJUSTABLE	
	public static  int MAX_VIPS_NUM=3; //maximum vIPS number in current node
	public static  int INIT_VIPS_NUM=1; //active vIPS number in initialization
	
	public static  double PARAM_CPUS=0.5; //the cpus parameter, i.e., each vIPS can use $PARAM_CPUS$ number of CPU resource
	//public static  int PARAM_ID_CPU=1;//cpu used by vips
	//public static final double PARAM_MEM=400;//memory used by vIPS
	
	public static int PORT_UFD=10001;
	public static int PORT_OVS_VSERVER=2;
	

}
