package com.boxfox.vertx.middleware;

import com.boxfox.vertx.router.Param;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;

public class BaseHandlerImpl implements BaseHandler {
    private Object instance;
    private Method m;

    public BaseHandlerImpl(Object instance, Method m) {
        this.instance = instance;
        this.m = m;
    }

    @Override
    public void handle(RoutingContext ctx) {
        List<Object> arguments = new ArrayList<>();
        List<String> emptyArguments = new ArrayList<>();
        Arrays.stream(m.getParameters()).forEach(param -> {
            Class<?> paramClass = param.getType();
            if (paramClass.equals(RoutingContext.class)) {
                arguments.add(ctx);
            } else {
                Object paramData = null;
                if (param.getAnnotation(Param.class) != null) {
                    String paramName = param.getAnnotation(Param.class).name();
                    paramData = getParameterFromBody(ctx, paramName, paramClass);
                    if (paramData == null)
                        paramData = castingParameter(ctx.pathParam(paramName), paramClass);
                    if (paramData == null && ctx.queryParam(paramName).size() > 0)
                        paramData = castingParameter(ctx.queryParam(paramName).get(0), paramClass);
                    if (paramData == null) {
                        emptyArguments.add(paramName);
                    }
                }
                arguments.add(paramData);
            }
        });
        try {
            if (emptyArguments.size() > 0) {
                throw new IllegalArgumentException();
            }
            m.invoke(instance, arguments.toArray());
        } catch (IllegalAccessException e) {
            ctx.response().setStatusCode(500).end();
            Logger.getRootLogger().error(e);
        } catch (IllegalArgumentException e) {
            String message = String.format("Illegal arguments %s", String.join(",", emptyArguments));
            ctx.response().setStatusCode(400).end(message);
            Logger.getRootLogger().error(message, e);
        } catch (InvocationTargetException e) {
            ctx.response().setStatusCode(500).end();
            Logger.getRootLogger().error(e.getTargetException());
        }
    }

    private Object castingParameter(String str, Class<?> paramType) {
        Object paramData = null;
        if (str != null) {
            if (paramType.equals(Integer.class) || paramType.equals(int.class)) {
                paramData = Integer.valueOf(str);
            } else if (paramType.equals(Boolean.class) || paramType.equals(boolean.class)) {
                paramData = Boolean.valueOf(str);
            } else if (paramType.equals(Double.class) || paramType.equals(double.class)) {
                paramData = Double.valueOf(str);
            } else if (paramType.equals(Float.class) || paramType.equals(float.class)) {
                paramData = Float.valueOf(str);
            } else if (paramType.equals(JsonObject.class)) {
                paramData = new JsonObject(str);
            } else if (paramType.equals(JsonArray.class)) {
                paramData = new JsonArray(str);
            } else {
                paramData = str;
            }
        }
        return paramData;
    }

    private Object getParameterFromBody(RoutingContext ctx, String paramName, Class<?> paramType) {
        Object paramData = null;
        String data = ctx.request().getFormAttribute(paramName);
        if (data != null) {
            if (paramType.equals(String.class)) {
                paramData = data;
            } else if (paramType.equals(Integer.class) || paramType.equals(int.class)) {
                paramData = Integer.valueOf(data);
            } else if (paramType.equals(Boolean.class) || paramType.equals(boolean.class)) {
                paramData = Boolean.valueOf(data);
            } else if (paramType.equals(Double.class) || paramType.equals(double.class)) {
                paramData = Double.valueOf(data);
            } else if (paramType.equals(Float.class) || paramType.equals(float.class)) {
                paramData = Float.valueOf(data);
            } else if (paramType.equals(JsonObject.class)) {
                paramData = new JsonObject(data);
            } else if (paramType.equals(JsonArray.class)) {
                paramData = new JsonArray(data);
            } else if (paramType.equals(byte[].class)) {
                paramData = Byte.valueOf(data);
            }
        }
        return paramData;
    }
}
