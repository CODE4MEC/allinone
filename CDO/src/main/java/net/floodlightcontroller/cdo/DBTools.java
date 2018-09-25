package net.floodlightcontroller.cdo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map.Entry;

import net.floodlightcontroller.cdo.types.AverageVStats;
import net.floodlightcontroller.cdo.types.DefaultValues;
import net.floodlightcontroller.cdo.types.UrlType;
import net.floodlightcontroller.cdo.types.VipsTrafficInfo;

public  class DBTools {

	public DBTools() {
		// TODO Auto-generated constructor stub
	}
	
	public static int CreatingDB(String dbname) throws IOException
	{
		String url=UrlType.URL_DB+UrlType.SUFFIX_DB_QUERY;
//		String command=  "curl -i -XPOST "
//						+url
//						+" --data-urlencode"
//						+" \'q=CREATE DATABASE "+
//						dbname
//						+"\'";		
		String[] command= {"curl",
							"-i", 
							"-XPOST",
							url,
							"--data-urlencode",
							"q=CREATE DATABASE "+dbname};
		Process p=Runtime.getRuntime().exec(command);
		String ret=GetRet(p);//GetErrorStr(p);
		if((GetReturnCode(ret)>299)||(GetReturnCode(ret)<200))
		{
			System.out.println("[error] unable to create db,ret="+ret);
			return -1;
		}
		
		//TEST
		float a=(float) 0.1;
		AverageVStats avs=new AverageVStats("Node1",a,a);
		WriteAverageVStats(avs,dbname);
		
		return 0;
	}
	public static String GetErrorStr(Process p) throws IOException
	{
		String ret="";
		InputStream is =p.getErrorStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String s = null;
		while ((s = reader.readLine()) != null) {
			ret=ret + s +"\n";
		}
		return ret;
	}
	public static String GetRet(Process p)
	{
		String ret="";
		try
		{
			// 
			InputStream is = p.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			
			p.waitFor();
			if (p.exitValue() != 0) {
			    // 
			    // 
				System.out.println("error in executing command");
				return "";
			}
			// 
			String s = null;
			while ((s = reader.readLine()) != null) {
				ret=ret + s +"\n";
			}
		}
		catch(Exception e)
		{
			;
		}
		return ret;
	}
	


	public static int WriteAverageVStats(AverageVStats avs,String dbname) throws IOException
	{
		//long timestamp=System.currentTimeMillis();
		String url=UrlType.URL_DB+UrlType.SUFFIX_DB_WRITE+"?db="+dbname;
	//average vips cpu data	
		{
//			String[] command_cpu= {"curl", 
//									"-i",
//									"-XPOST",
//									url,
//									"--data-binary",
//									DefaultValues.DB_MEASUREMENT_AVS_CPU +","
//											+ "host="+avs.getMecNodeID()+" "
//											+ "value="+avs.getAverageCPUPercent()+" "
//											+ timestamp};
			String[] command_cpu= {"curl", 
					"-i",
					"-XPOST",
					url,
					"--data-binary",
					DefaultValues.DB_MEASUREMENT_AVS_CPU +","
							+ "host="+avs.getMecNodeID()+" "
							+ "value="+avs.getAverageCPUPercent()
							};
			Process p=Runtime.getRuntime().exec(command_cpu);
			String ret=GetRet(p);
			if((GetReturnCode(ret)>299)||(GetReturnCode(ret)<200))
			{
				System.out.println("[error] unable to write avs_cpu to db,ret="+ret);
				return -1;
			}
		}
	//average vips mem data			
		{
//			String[] command_mem= {"curl", 
//					"-i",
//					"-XPOST",
//					url,
//					"--data-binary",
//					DefaultValues.DB_MEASUREMENT_AVS_MEM +","
//							+ "host="+avs.getMecNodeID()+" "
//							+ "value="+avs.getAverageMemPercent()+" "
//							+ timestamp};
			String[] command_mem= {"curl", 
					"-i",
					"-XPOST",
					url,
					"--data-binary",
					DefaultValues.DB_MEASUREMENT_AVS_MEM +","
							+ "host="+avs.getMecNodeID()+" "
							+ "value="+avs.getAverageMemPercent()
							};
			Process p=Runtime.getRuntime().exec(command_mem);
			String ret=GetRet(p);
			if((GetReturnCode(ret)>299)||(GetReturnCode(ret)<200))
			{
				System.out.println("[error] unable to write avs_mem to db,ret="+ret);
				return -1;
			}
		}

		return 0;
	}
	
	public static int WriteVipsTrafficInfo(VipsTrafficInfo vt, String dB_NAME) throws IOException {
		if(vt.GetInfo().size()==0)
		{
			return 0;
		}
		
		String url=UrlType.URL_DB+UrlType.SUFFIX_DB_WRITE+"?db="+dB_NAME;
		String raw_command="curl&&-i&&-XPOST&&"+url+"&&--data-binary&&";
		for(Entry<String, Long> entry:vt.GetInfo().entrySet())
		{
			String name=entry.getKey();
			Long traffic=entry.getValue();
			raw_command=raw_command
						+DefaultValues.DB_MEASUREMENT_VIPS_TRAFFIC+","
						+"host="+name+" "
						+"value="+traffic
						+"\n ";
		}
		String[] command=raw_command.split("\\&\\&");
		Process p=Runtime.getRuntime().exec(command);
		String ret=GetRet(p);
		if((GetReturnCode(ret)>299)||(GetReturnCode(ret)<200))
		{
			System.out.println("[error] unable to write avs_cpu to db,ret="+ret);
			return -1;
		}
		return 0;
	}
	
	public static int WriteEntry(String tag, double value, String measurement, String db_NAME) throws IOException
	{
		String url=UrlType.URL_DB+UrlType.SUFFIX_DB_WRITE+"?db="+db_NAME;
		String[] command_mem= {"curl", 
				"-i",
				"-XPOST",
				url,
				"--data-binary",
				measurement +","
						+ "host="+tag+" "
						+ "value="+value
						};
		Process p=Runtime.getRuntime().exec(command_mem);
		String ret=GetRet(p);
		if((GetReturnCode(ret)>299)||(GetReturnCode(ret)<200))
		{
			System.out.println("[error] unable to write "+measurement+" to "+db_NAME+",ret="+ret);
			return -1;
		}
		return 0;
	}

	
	public static int GetReturnCode(String str)
	{
		String[] tokens=str.split(" ");
		try
		{
			return Integer.parseInt(tokens[1]);
		}
		catch ( NumberFormatException e)
		{
			return -1;
		}
		
	}


	
}
