package com.rpcf.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.rpcf.api.RpcException;
import com.rpcf.client.Client;
import com.rpcf.common.Request;
import com.rpcf.util.IPEnum;
import com.rpcf.util.RpcUtil;

public class BaseConsumerProxy {
	
	private static final AtomicLong uniqueId = new AtomicLong(0);
	
	public Object doInterval(String interfaceName, String invokeMethod, Object[] objs) throws Throwable{
		List<Class<?>> clazzs = new ArrayList<Class<?>>(objs.length);
		List<Object> params = new ArrayList<Object>();
		for(Object obj: objs) {
			clazzs.add(obj.getClass());
			params.add(obj);
		}
		Request request = new Request();
		request.setInterfaceName(interfaceName);
		request.setInvokeMethod(invokeMethod);
		request.setParameterTypes(clazzs);
		request.setParameters(params);
		request.setId(uniqueId.getAndIncrement());
		
		String projectCode = (String)objs[0];
		String profile = (String)objs[1];
		String ip = IPEnum.getIPbyProjectcodeAndProfile(projectCode, profile);
		
		Client client = Client.clients.get(ip+":"+ RpcUtil.getRpcPort());
		if(client==null) {
			try {
				client = new Client(ip, RpcUtil.getRpcPort());
			} catch (Throwable e) {
				e.printStackTrace();
				throw new RpcException("创建client失败:" + ip + ":" + RpcUtil.getRpcPort());
			}
		}
		DefaultFuture future = client.send(request);
		return future.get().getResult();
	}
	
}
