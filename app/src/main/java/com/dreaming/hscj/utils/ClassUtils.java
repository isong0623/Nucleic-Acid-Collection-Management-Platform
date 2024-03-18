package com.dreaming.hscj.utils;

public class ClassUtils {
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
}
