package com.rpcf.util;

public class RpcUtil implements DefaultConfig{
	
	private static int port;
	
	public static int getRpcPort(){
		return port==0?DEFAULT_RCP_PORT:port;
	}
	
}
