package com.dreaming.hscj;

import com.dreaming.hscj.template.api.ApiProvider;

import org.junit.Test;

public class ApiTest {
    int target = 275;

    int query(int page){
        int tar = target/10;
        if(tar == page) return target % 10;
        if(page<tar) return 10;
        return 0;
    }


    public int test(){
        int l = 0;
        int r = 100000;
        int m = 500;
        boolean bIsFirst = true;
        int result = 0;
        while(l<r){
            m = bIsFirst ? 500 : ((l+r)/2);
            bIsFirst = false;

            int q = query(m);
            if(q == 10){
                l = m+1;
                result = l * 10;
                continue;
            }
            if(q == 0){
                r = m;
                continue;
            }
            result = m*10 + q;
            break;
        }

        return result;
    }


    @Test
    public void doTest(){
        for(int i=0;i<1000;++i){
            target = i;
            if(target != test()){
                System.out.println(target);
            }
        }
    }

    @Test
    public void myTest(){
        //QD(?<a>([0-9]{8}))
        //QD1${a}
        System.out.println("QD00852145".replaceAll("QD(?<a>([0-9]{8}))","QD1${a}"));
    }
}
