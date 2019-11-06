package com.meng.api.core;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ApiHanderAdapter {

    private ApplicationContext applicationContext;

    String apiName;  //bit.api.user.getUser

    String targetName; //ioc bean 名称

    Object target; // UserServiceImpl 实例
    Method targetMethod; // 目标方法 getUser
    APIMapping apiMapping;



    public ApiHanderAdapter(ApplicationContext context){
        Assert.notNull(context, "ApiHanderAdapter上下文不能为空");
        this.applicationContext = context;
    }


    public Object run(Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (target == null) { // spring ioc 容器里面去服务Bean 比如GoodsServiceImpl
            target = applicationContext.getBean(targetName);
        }
        return targetMethod.invoke(target, args);
    }

    public Class<?>[] getParamTypes() {
        return targetMethod.getParameterTypes();
    }

    public String getApiName() {
        return apiName;
    }

    public String getTargetName() {
        return targetName;
    }

    public Object getTarget() {
        return target;
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    public APIMapping getApiMapping() {
        return apiMapping;
    }
}
