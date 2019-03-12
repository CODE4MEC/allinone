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
			
			//read the result of the execution
			InputStream is = p.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			
			p.waitFor();
			if (p.exitValue() != 0) {
			    //failed in execution
			    //read error stream
				InputStream stderr = p.getErrorStream();
	            InputStreamReader isr = new InputStreamReader(stderr);
	            BufferedReader br = new BufferedReader(isr);
	            String line = null;
	            String errmsg="error in executing command:"+command+"\n stderr=\n";
	            while ((line = br.readLine()) != null)
	                errmsg+=(line);
	            logger.error(errmsg);			
				return "";
			}
			//print the response message
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
