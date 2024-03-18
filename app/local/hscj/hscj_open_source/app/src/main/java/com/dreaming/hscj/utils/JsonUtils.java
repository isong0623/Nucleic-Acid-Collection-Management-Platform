package com.dreaming.hscj.utils;

import java.util.ArrayList;
import java.util.List;

import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

public class JsonUtils {
    public static List<ESONObject> parseToList(ESONArray array){
        List<ESONObject> lst = new ArrayList<>();

        for(int i=0,ni=array==null?0:array.length();i<ni;++i){
            lst.add(array.getArrayValue(i,new ESONObject()));
        }

        return lst;
    }

    public static <T> List<T> parse2List(ESONArray array,T defaultValue){
        List<T> lst = new ArrayList<>();

        for(int i=0,ni=array==null?0:array.length();i<ni;++i){
            lst.add(array.getArrayValue(i,defaultValue));
        }

        return lst;
    }

    public static ESONArray parse2Array(List lst){
        ESONArray array = new ESONArray();
        for(int i=0,ni=lst==null?0:lst.size();i<ni;++i){
            array.putValue(lst.get(i));
        }
        return array;
    }
}
