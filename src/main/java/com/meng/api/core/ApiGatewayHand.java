package com.meng.api.core;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.meng.api.common.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.*;


public class ApiGatewayHand implements InitializingBean, ApplicationContextAware {

    private static final String METHOD = "method";
    private static final String PARAMS = "params";

    ApiStore apiSorte;

    TokenService tokenService;

    final ParameterNameDiscoverer parameterUtil;

    public ApiGatewayHand(){
        parameterUtil = new LocalVariableTableParameterNameDiscoverer();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        apiSorte.loadApiFromSpringBeans();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        apiSorte = new ApiStore(applicationContext);
    }

    public void handler(HttpServletRequest request, HttpServletResponse response) {
        // 系统参数验证
        String params = request.getParameter(PARAMS);
        String method = request.getParameter(METHOD);
        Object result;
        ApiHanderAdapter apiHanderAdapter = null;
        try {
            //验证参数是否正确
            apiHanderAdapter = SystemParamsValidate(request);
            ApiRequest apiRequest = null;
            //构建apiRequest
            apiRequest = buildApiRequest(request);
            //TODO 签名验证
            if (apiRequest.getAccessToken() != null){
                signCheck(apiRequest);
            }

            if(apiHanderAdapter.getApiMapping().useLogin()){
                if(!apiRequest.isLogin()){
                    throw new ApiException("009","调用失败：用户未登陆");
                }
            }
            System.out.println("请求接口={" + method + "} 参数=" + params);
            Object[] args = buildParams(apiHanderAdapter, params, request, response, apiRequest);
            result = apiHanderAdapter.run(args);
        } catch (ApiException e){
            response.setStatus(500);// 封装异常并返回
            System.out.println("调用接口={" + method + "}异常  参数=" + params + e.getMessage());
            result = handleError(e);
        } catch (Exception e){
            response.setStatus(500);// 封装业务异常并返回
            System.out.println("其他异常" + e.getMessage());
            result = handleError(e);
        }
        // 统一返回结果
        returnResult(result, response);
    }

    private ApiRequest signCheck(ApiRequest request) throws ApiException {
        Token token = tokenService.getToken(request.getAccessToken());
        if (token == null) {
            throw new ApiException("验证失败：指定'Token'不存在");
        }
        if (token.getExpiresTime().before(new Date())) {
            throw new ApiException("验证失败：指定'Token'已失效");
        }
        // 生成签名
        String methodName = request.getMethodName();
        String accessToken = token.getAccessToken();
        String secret = token.getSecret();
        String params = request.getParams();
        String timestamp = request.getTimestamp();
        String sign = Md5Util.MD5(secret + methodName + params + token + timestamp + secret);

        if (!sign.toUpperCase().equals(request.getSign())) {
            throw new ApiException("验证失败：签名非法");
        }

        // 时间验证
        if (Math.abs(Long.valueOf(timestamp) - System.currentTimeMillis()) > 10 * 60 * 1000) {
            throw new ApiException("验证失败：签名失效");
        }

        request.setLogin(true);
        request.setMemberId(token.getMemberId());
        return request;
    }

    private ApiRequest buildApiRequest(HttpServletRequest request) {
        ApiRequest apiRequest = new ApiRequest();
        apiRequest.setAccessToken(request.getParameter("token"));
        apiRequest.setSign(request.getParameter("sign"));
        apiRequest.setTimestamp(request.getParameter("timestamp"));
        apiRequest.seteCode(request.getParameter("eCode"));
        apiRequest.setuCode(request.getParameter("uCode"));
        apiRequest.setParams(request.getParameter("params"));
        return apiRequest;
    }



    private Object[] buildParams(ApiHanderAdapter apiHanderAdapter, String params,
                                 HttpServletRequest request, HttpServletResponse response, ApiRequest apiRequest) throws ApiException {
        Map<String, Object> map = null;
        try {
            map = UtilJson.toMap(params);
        } catch (IllegalArgumentException e){
            throw new ApiException("调用失败：json字符串格式异常，请检查params参数 ");
        }
        if(map == null){
            map = new HashMap<>();
        }
        Method method = apiHanderAdapter.getTargetMethod();
        List<String> paramNames = Arrays.asList(parameterUtil.getParameterNames(method));
        Class<?>[] paramTypes = method.getParameterTypes();
        //验证方法参数格式是否正确
        for(Map.Entry<String, Object> m : map.entrySet()){
            if(!paramNames.contains(m.getKey())){
                throw new ApiException("调用失败：接口不存在‘" + m.getKey() + "’参数");
            }
        }
        Object[] args = new Object[paramTypes.length];
        for(int i = 0; i< paramTypes.length; i++) {
            if(paramTypes[i].isAssignableFrom(HttpServletRequest.class)){
                args[i] = request;
            } else if(paramTypes[i].isAssignableFrom(ApiRequest.class)){
                args[i] = apiRequest;
            } else if(paramTypes[i].isAssignableFrom(HttpServletResponse.class)){
                args[i] = response;
            } else if(map.containsKey(paramNames.get(i))){
                try {
                    args[i] = convertJsonToBean(map.get(paramNames.get(i)), paramTypes[i]);
                }catch (Exception e){
                    throw new ApiException("调用失败：指定参数格式错误或值错误‘" + paramNames.get(i) + "’"
                            + e.getMessage());
                }
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    private <T> Object convertJsonToBean(Object val, Class<T> targetClass) {
        Object result = null;
        if (val == null) {
            return null;
        } else if (Integer.class.equals(targetClass)) {
            result = Integer.parseInt(val.toString());
        } else if (Long.class.equals(targetClass)) {
            result = Long.parseLong(val.toString());
        } else if (Date.class.equals(targetClass)) {
            if (val.toString().matches("[0-9]+")) {
                result = new Date(Long.parseLong(val.toString()));
            } else {
                throw new IllegalArgumentException("日期必须是长整型的时间戳");
            }
        } else if (String.class.equals(targetClass)) {
            if (val instanceof String) {
                result = val;
            } else {
                throw new IllegalArgumentException("转换目标类型为字符串");
            }
        } else {
            result = UtilJson.convertValue(val, targetClass);
        }
        return result;
    }

    private ApiHanderAdapter SystemParamsValidate(HttpServletRequest request) throws ApiException {
        String params = request.getParameter(PARAMS);
        String method = request.getParameter(METHOD);
        ApiHanderAdapter apiHanderAdapter = null;
        if((method == null) || method.trim().equals("")){
            throw  new ApiException("调用失败：参数'method'为空");
        } else if(params == null || params.trim().equals("")){
            throw new ApiException("调用失败：参数'params'为空");
        }
        apiHanderAdapter = apiSorte.findApiHanderAdapter(method);
        if(apiHanderAdapter == null){
            throw new ApiException("调用失败：指定API不存在，API:" + method);
        }
        return apiHanderAdapter;
    }


    private Object handleError(Throwable throwable) {
        String code = "";
        String message = "";

        if (throwable instanceof ApiException) {
            code = "0001";
            message = throwable.getMessage();
        } // 扩展异常规范
        else {
            code = "0002";
            message = throwable.getMessage();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("error", code);
        result.put("msg", message);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(out);
        throwable.printStackTrace(stream);
//        result.put("stack", out.toString());
        return result;
    }

    private void returnResult(Object result, HttpServletResponse response) {
        try {
            UtilJson.JSON_MAPPER.configure(
                    SerializationFeature.WRITE_NULL_MAP_VALUES, true);
            String json = UtilJson.writeValueAsString(result);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html/json;charset=utf-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            if (json != null){
                response.getWriter().write(json);
            }
        } catch (IOException e) {
            System.out.println("服务中心响应异常" +  e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
