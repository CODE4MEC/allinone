package com.iie.devy.Cxxpure.CommandTools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class CommandExecution {
	
	protected Runtime run;
	private static final Logger logger=Logger.getLogger(CommandExecution.class);
	
	public CommandExecution()
	{
		run=Runtime.getRuntime();
	}
	
	protected synchronized String ExecCommandWithSudo(String command)
	{
		//for mac
//		command = "/usr/local/bin/sudo "+command;
		//for linux
		command = "/usr/bin/sudo "+command;
		return ExecCommand(command);
	}
	
	protected synchronized String ExecCommand(String command)  
	{	
		String ret="Error";
		Process p;
		try {
			p = run.exec(command);
			
			//读取命令的输出信息
			InputStream is = p.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			
			p.waitFor();
			if (p.exitValue() != 0) {
			    //说明命令执行失败
			    //可以进入到错误处理步骤中
				logger.error("error in executing: "+command);
				return "";
			}
			//打印输出信息
			ret="";
			String s = null;
			while ((s = reader.readLine()) != null) {
				ret=ret + s +"\n";
			}
		
		} catch (IOException | InterruptedException e) {
			logger.error("error in executing:"+command);
			e.printStackTrace();
		}

		
		return ret;
	}
	
	protected synchronized void ExecCommandWithoutRet(String command)  
	{	
		try {
			run.exec(command);	
		} catch (IOException  e) {
			logger.error("[IOException] error in executing:"+command);
			e.printStackTrace();
		}
	}

}
