package com.dreaming.hscj.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by zhaoliangtai on 2018/3/13.
 */

public class ReflectUtils {



    public static <T> T getParameterizeTypeInstance(Object o, int index) {
        ParameterizedType parameterize = (ParameterizedType) o.getClass().getGenericSuperclass();

        try {
            return ((Class<T>) parameterize.getActualTypeArguments()[index]).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Type[] types = parameterize.getActualTypeArguments();
            for(Type type : types){
                return ((Class<T>) type).newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }
}
