package com.dreaming.hscj.core;

import android.os.Bundle;
import android.os.Parcelable;

import java.io.Serializable;

public class BundleBuilder {
    Bundle mBundle = new Bundle();
    public BundleBuilder(){}

    Class getBaseClass(String type){
        if(type == null) return null;
        switch (type){
            case "int":
                return int.class;
            case "short":
                return short.class;
            case "long":
                return long.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "byte":
                return byte.class;
            case "float":
                return float.class;
            case "boolean":
                return boolean.class;
        }

        return null;
    }

    public static BundleBuilder create(){
        return new BundleBuilder();
    }

    public static <T> BundleBuilder create(String key, T value){
        return new BundleBuilder().put(key,value);
    }

    public <T> BundleBuilder put(String key, T value){
        if(value == null){ return this; }

        try {
            //获取类名
            String targetClassName = value
                    .getClass()
                    .getSimpleName()
                    .replace("[]","Array");
            if(targetClassName.contains(".")){
                targetClassName = targetClassName.substring(targetClassName.lastIndexOf(".")+1);
            }
            //获取方法名
            String targetMethodName = String.format("put%s%s",
                    targetClassName.substring(0,1).toUpperCase(),
                    targetClassName.substring(1));
            //根据类名找到被调用的类
            Class targetClass = getBaseClass(targetClassName.toLowerCase());
            if(targetClass==null){
                targetClass = value.getClass();
            }
            //调用Bundle.putXXX()方法
            Bundle
                .class
                .getMethod(targetMethodName, new Class[]{String.class, targetClass})
                .invoke(mBundle,key,value);
        } catch (Exception e) {
            if(Serializable.class.isAssignableFrom(value.getClass())){
                try{ mBundle.putSerializable(key, (Serializable) value); } catch (Exception e2){}
            }
            else if(Parcelable.class.isAssignableFrom(value.getClass())){
                try{ mBundle.putParcelable(key, (Parcelable) value); } catch (Exception e2){}
            }
        }
        return this;
    }

    public Bundle build(){ return mBundle; }
}