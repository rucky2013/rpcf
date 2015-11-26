package com.rpcf.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RpcfClassLoader {
	
	/**
	 * 反射调用静态方法，threadcontextclassloader
	 * @param classFullName
	 * @param methodName
	 * @param parameters
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws ClassNotFoundException 
	 */
	public static Object invokeStaticMethod(String classFullName, String methodName, Object[] parameters) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
		Class clazz = getTCL().loadClass(classFullName);
		Class[] parameterTypes = null;
		if(parameters != null) {
			parameterTypes = new Class[parameters.length];
			int i = 0;
			for(Object parameter: parameters) {
				parameterTypes[i++] = parameter.getClass();
			}
		}
		Method getSidMethod = clazz.getMethod(methodName, parameterTypes);
		return getSidMethod.invoke(clazz, parameters);
	}
	
	protected static ClassLoader getTCL() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = null;
		method = Thread.class.getMethod("getContextClassLoader", null);
		return (ClassLoader)method.invoke(Thread.currentThread(), null);
	}
}
