package com.iie.devy.Cxxpure.Default;

public class ParamDefault {

//FINAL
	public static final int SAMPLE_PERIOD_TYPE_IM = 5000;//vips状态采样周期,用于TdStatListen typeIM
	public static final int SAMPLE_PERIOD_TYPE_CT = 1000;
	
	public static final int MAX_FOREIGN_HELPING_VIPS=100;//单个节点向别人借的vips数不应超过该值
	
	//vips类型
	public static final int TYPE_HELPING_VIPS=0;
	public static final int TYPE_LOCAL_VIPS=1;
	public static final int TYPE_UNKNOWN_VIPS=2;
	
	//ALARM RELATED
	public static final int TH_CPU_ALARM=80;
	//public static final int TH_MEM_ALARM=0;
	public static final int TH_ALARM_PERIOD = 15000;
	public static final int TH_ALARM_SLOT_NUM_TYPE_IM=TH_ALARM_PERIOD/SAMPLE_PERIOD_TYPE_IM;//连续x个时隙都报警则上报request
	public static final int TH_ALARM_SLOT_NUM_TYPE_CT=TH_ALARM_PERIOD/SAMPLE_PERIOD_TYPE_CT;//连续x个时隙都报警则上报request	
	//RELEASE RELATED
	public static final int TH_CPU_RELEASE_LOCAL=70; //适用于本地vips释放，SUM(Overhead)<(N-1)*TH_CPU_RELEASE_LOCAL
	public static final int TH_CPU_RELEASE_HELPING=20;//适用于helping vips释放, Overhead<TH_CPU_RELEASE_HELPING
	//public static final int TH_MEM_RELEASE=0;
	public static final int TH_RELEASE_PERIOD = 15000;
	public static final int TH_RELEASE_SLOT_NUM_TYPE_IM = TH_RELEASE_PERIOD/SAMPLE_PERIOD_TYPE_IM;//连续x个时隙都走低则释放
	public static final int TH_RELEASE_SLOT_NUM_TYPE_CT = TH_RELEASE_PERIOD/SAMPLE_PERIOD_TYPE_CT;
	//ADJUSTABLE	
	public static  int MAX_VIPS_NUM=3; //支持的最大的vips数量
	public static  int INIT_VIPS_NUM=1; //初始化时连接UFD的vips的数量
	
	public static  double PARAM_CPUS=0.1; //每个vIPS可以使用的cpu数量
	//public static  int PARAM_ID_CPU=1;//vips所在的cpu
	//public static final double PARAM_MEM=400;//每个vIPS可以使用的内存大小
	
	public static int PORT_UFD=10001;
	public static int PORT_OVS_VSERVER=2;
	

}
