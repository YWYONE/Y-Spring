package com.example.yspringcore.ioc.scan;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.util.*;
import java.util.function.Function;

/**
 * maintain  all  properties
 */
@Slf4j
public class PropertyResolver {

    Map<String,String> properties=new HashMap<>();
    Map<Class<?>, Function<String,?>> converters=new HashMap<>();
    public PropertyResolver(Properties props) {
        this.properties.putAll(System.getenv());
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }
        // register converters:
        converters.put(String.class, s -> s);
        converters.put(boolean.class, s -> Boolean.parseBoolean(s));
        converters.put(Boolean.class, s -> Boolean.valueOf(s));

        converters.put(byte.class, s -> Byte.parseByte(s));
        converters.put(Byte.class, s -> Byte.valueOf(s));

        converters.put(short.class, s -> Short.parseShort(s));
        converters.put(Short.class, s -> Short.valueOf(s));

        converters.put(int.class, s -> Integer.parseInt(s));
        converters.put(Integer.class, s -> Integer.valueOf(s));

        converters.put(long.class, s -> Long.parseLong(s));
        converters.put(Long.class, s -> Long.valueOf(s));

        converters.put(float.class, s -> Float.parseFloat(s));
        converters.put(Float.class, s -> Float.valueOf(s));

        converters.put(double.class, s -> Double.parseDouble(s));
        converters.put(Double.class, s -> Double.valueOf(s));

        converters.put(LocalDate.class, s -> LocalDate.parse(s));
        converters.put(LocalTime.class, s -> LocalTime.parse(s));
        converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
        converters.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
        converters.put(Duration.class, s -> Duration.parse(s));
        converters.put(ZoneId.class, s -> ZoneId.of(s));
    }
    @Nullable
    public <T> T getProperty(String key,Class<T> targetType){
        String value =getProperty(key);
        if(value!=null){
            return convert(value,targetType);
        }else{
            return null;
        }

    }
    public <T> T convert(String value,Class<T> targetType) {
        Function<String, ?> converter = converters.get(targetType);
        if (converter == null){
            log.info("Unsupported value type: {}", targetType.getName());
            return null;
        }else{
            return (T)converter.apply(value);
        }
    }

    /**
     * converter extend
     * @param targetType
     * @param function
     * @param <T>
     */
    public <T> void RegisterConverter(Class<T> targetType,Function<String,T> function){
        converters.put(targetType,function);
    }

    /**
     * the core get function
     * @param key
     * @return
     */
    @Nullable
    public String getProperty(String key){
        DefaultProperty defaultProperty=parsePropertyExpr(key);
        if(defaultProperty!=null){//key is a expr ${}
            if(defaultProperty.defaultValue()!=null){
                return getProperty(defaultProperty.key(), defaultProperty.defaultValue());
            }else{
                return getProperty(defaultProperty.key());
            }
        }
        // key is not a expr
        String value=properties.get(key);
        if(value!=null){
            return value;
        }else{
            log.info("{} 's value not found",key);
            return value;
        }
    }
    public String getProperty(String key,String defaultValue){
        String value=getProperty(key);
        return value==null ? parseValue(defaultValue):value;
    }

    /**
     * parse ${}
     * @param expr
     * @return
     */
    public DefaultProperty parsePropertyExpr(String expr){
        if(expr.startsWith("${")&&expr.endsWith("}")){
            int index=expr.indexOf(':');
            if(index==-1){
                //${key}
                return new DefaultProperty(expr.substring(2,expr.length()-1),null);
            }else{
                //${key:defaultValue}
                return new DefaultProperty(expr.substring(2,index),expr.substring(index+1,expr.length()-1));
            }
        }
        return null;
    }

    /**
     * when key's value not found
     * parse recursion default value  eg. ${key1:${key2:value}}
     * @param defaultValue
     * @return
     */
    public String parseValue(String defaultValue){
        DefaultProperty value=parsePropertyExpr(defaultValue);
        if(value!=null){//value is a expr ${}
            if(value.defaultValue()!=null){
                return getProperty(value.key(), value.defaultValue());
            }else{
                return getProperty(value.key());
            }
        }else{//value is not a expr ${}
            return defaultValue;
        }
    }





}
