package com.meng.api.core;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.*;

public class ApiStore {

    private ApplicationContext applicationContext;
    // API 接口存储
    private HashMap<String, ApiHanderAdapter> apiMap = new HashMap<String, ApiHanderAdapter>();

    public ApiStore(ApplicationContext applicationContext) {
        Assert.notNull(applicationContext, "ApiStore上下文不能为空");
        this.applicationContext = applicationContext;
    }

    /**
     * 基于spring ioc 容器里面的bean 查找对应的api方法
     */
    public void loadApiFromSpringBeans(){
        String[] names = applicationContext.getBeanDefinitionNames();
        Class<?> type;
        for (String beanName : names){
            type = applicationContext.getType(beanName);
            for(Method method : type.getDeclaredMethods()){
                //判斷方法上是否有apimapping注解
                APIMapping apiMapping = method.getAnnotation(APIMapping.class);
                if(apiMapping != null){
                    //添加api映射到集合
                    addApiItem(apiMapping, beanName, method);
                }
            }
            
        }
    }

    private void addApiItem(APIMapping apiMapping, String beanName, Method method) {
        ApiHanderAdapter apiHanderAdapter = new ApiHanderAdapter(applicationContext);
        apiHanderAdapter.apiMapping  = apiMapping;
        apiHanderAdapter.apiName = apiMapping.value();
        apiHanderAdapter.targetName = beanName;
        apiHanderAdapter.targetMethod = method;
        apiMap.put(apiMapping.value(), apiHanderAdapter);

    }

    public ApiHanderAdapter findApiHanderAdapter(String apiName) {
        return apiMap.get(apiName);
    }

    public ApiHanderAdapter findApiHanderAdapter(String apiName, String version) {
        return (ApiHanderAdapter) apiMap.get(apiName + "_" + version);
    }

    public List<ApiHanderAdapter> findApiHanderAdapters(String apiName) {
        if (apiName == null) {
            throw new IllegalArgumentException("api name must not null!");
        }
        List<ApiHanderAdapter> list = new ArrayList<ApiHanderAdapter>(20);
        for (ApiHanderAdapter api : apiMap.values()) {
            if (api.apiName.equals(apiName)) {
                list.add(api);
            }
        }
        return list;
    }

    public List<ApiHanderAdapter> getAll() {
        List<ApiHanderAdapter> list = new ArrayList<>();
        list.addAll(apiMap.values());
        Collections.sort(list, Comparator.comparing(ApiHanderAdapter::getApiName));
        return list;
    }

    public boolean containsApi(String apiName, String version) {
        return apiMap.containsKey(apiName + "_" + version);
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    
    
}
