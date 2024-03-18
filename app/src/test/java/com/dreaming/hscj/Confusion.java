package com.dreaming.hscj;

import com.dreaming.hscj.utils.FileUtils;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

public class Confusion {

    static Set<String> whiteFileList = new HashSet<>(Arrays.asList(
            "cJSON",
            "dreaming"
    ));

    static Map<String,List<String>> confuser = new HashMap<>();
    @Test
    public void doConfuse(){
        String hPath = "D:\\My_Developments\\Dreaming\\flutter_tests\\hscj\\libSecurity\\src\\main\\src\\headers";
        String sPath = "D:\\My_Developments\\Dreaming\\flutter_tests\\hscj\\libSecurity\\src\\main\\src\\sources";

        File fHDir = new File(hPath);
        File fSDir = new File(sPath);

//        readDirFiles(fHDir);
        readDirFilesAndFindFunctions(fHDir);
        readDirFilesAndFindFunctions(fSDir);
        readDirFilesAndConfuse(fHDir);
        readDirFilesAndConfuse(fSDir);

        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String,Map<String,Integer>> entry : mConfuseStr.entrySet()){
            String key = entry.getKey();
            Map<String,Integer> value = entry.getValue();
            if(value!=null){
                for(Map.Entry<String,Integer> entry1 : value.entrySet()){
                    sb.append(String.format("%s.%s %s\n",key,entry1.getKey(),encodeInteger(entry1.getValue())));
                    System.out.printf("%s.%s mapping %d %s\n",key,entry1.getKey(),entry1.getValue(),encodeInteger(entry1.getValue()));
                }
            }
        }
        FileUtils.writeToFile(new File("D:\\My_Developments\\Dreaming\\flutter_tests\\hscj\\libSecurity\\map.txt") ,sb);
    }

    public void readDirFilesAndFindFunctions(File fDir){
        File fs[] = fDir.listFiles();
        if(fs!=null){
            for(File f:fs){
                String fileName = f.getName();
                int idx = fileName.lastIndexOf(".");
                if(idx>-1){
                    fileName = fileName.substring(0,idx);
                }
                if(whiteFileList.contains(fileName)) continue;
                System.out.println("-----------------------------------------------------------");

                StringBuilder sb = readAllLines(f);
                findFunctions(fileName,sb);
                System.out.printf("read file:%s  part:%s \n",f.getName(),fileName);
                System.out.println();
            }
        }
    }

    public List<String> getIncludeHeaders(StringBuilder sbFileData){
        List<String> lst = new ArrayList<>();
        int index = 0;
        while(index>-1&&index<sbFileData.length()){
            int start = index;
            int end = sbFileData.indexOf("\n",start);
            if(end<0) break;
            String line = sbFileData.substring(start,end).trim().trim();

            if(line.startsWith("#include")&&line.contains("\"")){
                int pos = line.lastIndexOf("\"");
                if(pos>-1){
                    String headerName = "";
                    --pos;
                    while(pos>-1&&line.charAt(pos)!='\"'){
                        headerName = line.charAt(pos)+headerName;
                        --pos;
                    }
                    int idx1 = headerName.lastIndexOf("/");
                    int idx2 = headerName.lastIndexOf("\\");
                    headerName = headerName.substring(Math.max(idx1,idx2)+1,headerName.lastIndexOf("."));
                    lst.add(headerName);
                }
            }
            index = end+1;
        }
        return lst;
    }

    String strConfuseTable = "ZxlzhtsAiwyCvBTPGdKRIqJarLWOkXDHnSjbVYEefogmFupMQNcU";
    String encodeInteger(int num){
        return Integer.toHexString(16+num).toUpperCase();

//        final int N = strConfuseTable.length();
//        int n = num + 1;
//        Stack<Character> stack = new Stack();
//
//        while (n > 0) {
//            stack.push(strConfuseTable.charAt((n % N)));
//            n /= N;
//        }
//
//        StringBuilder sb = new StringBuilder();
//        while (!stack.empty()) {
//            sb.append(stack.pop());
//        }
//        return sb.toString();
    }
    int confuseIndex = 0;
    Map<String,Map<String,Integer>> mConfuseStr = new HashMap<>();
    public String getConfusedString(final String cppName, final String funcName){
        Map<String,Integer> map = mConfuseStr.get(cppName);
        if(map == null){
            map = new HashMap();
        }
        if(!map.containsKey(funcName)){
            map.put(funcName,confuseIndex++);
        }
        mConfuseStr.put(cppName,map);
        return "func_encoded_"+encodeInteger(map.get(funcName));
    }

    public void confuseByHeader(StringBuilder sbFileData,String cppName,String headerName){
        System.out.printf("confuse header in %s :%s\n", cppName, headerName);
        List<String> lst = confuser.get(headerName);

        if(lst!=null){
            lst.sort(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.length()==o2.length()?o1.compareTo(o2):o1.length()>o2.length()?-1:1;
                }
            });
            for(String funcName:lst){
                String confusedFunctionName = getConfusedString(headerName,funcName);
                for(int index = sbFileData.indexOf(funcName);index>-1; index = sbFileData.indexOf(funcName,index+1)){
                    int start = index;
                    int end = index+funcName.length();
                    int index2 = end;
                    while(index2<sbFileData.length()&&sbFileData.charAt(index2)==' ') ++index2;
                    if(index2>=sbFileData.length()) return;
                    if(!(start-1>-1 && sbFileData.charAt(start-1)=='('&&sbFileData.charAt(index2)==')'))//daemon.cpp
                    if(!((sbFileData.charAt(index2)!='(') ^ (sbFileData.charAt(index2)!='}'))) continue;

                    sbFileData.replace(start,end,confusedFunctionName);
                }
            }
        }
    }

    public void readDirFilesAndConfuse(File fDir){
        File fs[] = fDir.listFiles();
        if(fs!=null){
            for(File f:fs){
                String cppName = f.getName();
                int idx = cppName.lastIndexOf(".");
                if(idx>-1){
                    cppName = cppName.substring(0,idx);
                }
                StringBuilder sb = readAllLines(f);
                System.out.println("--------------------------------------------------------");
                System.out.println(f.getAbsolutePath());
                List<String> lstHeaders = getIncludeHeaders(sb);
                confuseByHeader(sb,cppName,cppName);
                for(String header:lstHeaders){
                    confuseByHeader(sb,cppName,header);
                }
                saveConfusedFile(sb,new File(f.getAbsolutePath().replace("src\\headers","cpp\\headers").replace("src\\sources","cpp\\sources")));
                System.out.println();
            }
        }
    }

    public void saveConfusedFile(StringBuilder sbFileData,File target){
        if(!target.getParentFile().exists()){
            target.getParentFile().mkdirs();
        }
        target.delete();
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(target);
            bw = new BufferedWriter(fw);
            bw.write(sbFileData.toString());
            bw.flush();
            System.out.printf("%s 文件混淆成功！\n",target);
        } catch (Exception e) {

        }
        finally {
            if(fw!=null){
                try { fw.close(); } catch (Exception e) { }
            }
            if(bw!=null){
                try { bw.close(); } catch (Exception e) { }
            }
        }
    }

    public StringBuilder readAllLines(File f){
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        FileReader fr = null;
        try{
            fr = new FileReader(f);
            br = new BufferedReader(fr);
            String strLine = null;
            while(null != (strLine = br.readLine())){
                sb.append(strLine);
                sb.append("\n");
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if(br!=null) {
                try { br.close(); } catch (Exception e) { }
            }
            if(fr!=null) {
                try { fr.close(); } catch (Exception e) { }
            }
        }
        return sb;
    }

    public String findFunction(String line){
        if(line==null) return null;
        line = line.trim();
        if(line.isEmpty()) return null;
        if(line.indexOf(" ")<0) return null;
        if(line.indexOf("(")<0) return null;
        if(line.indexOf(")")<0) return null;
        if(line.indexOf("{")<0) return null;
        if(line.startsWith("if")) return null;
        if(line.startsWith("else")) return null;
        if(line.startsWith("for")) return null;
        if(line.startsWith("switch")) return null;
        if(line.startsWith("catch")) return null;
        if(line.startsWith("while")) return null;
        if(line.startsWith("fputs")) return null;
        if(line.startsWith("LOG")) return null;
        if(line.startsWith("sprintf")) return null;
        if(line.startsWith("fclose")) return null;
        if(line.startsWith("exit")) return null;
        if(line.startsWith("sleep")) return null;
        if(line.startsWith("std::string(")) return null;
        if(line.startsWith("//")) return null;
        if(line.startsWith("fgets")) return null;
        if(line.startsWith("return")) return null;
        if(line.startsWith("std::istringstream is(")) return null;
        if(!Character.isLetter(line.charAt(0))) return null;

        StringBuilder funcName = new StringBuilder();
        int index = line.indexOf("(");
        while(true){
            --index;
            if(index<0) break;
            char c = line.charAt(index);
            if(c==' '||c=='*') break;
            funcName.insert(0,c);
        }
        return funcName.toString();
    }

    public void findFunctions(String fName,StringBuilder sbFileData){
        sbFileData.append("\n");
        int pIndex = 0;
        while(pIndex>-1){
            int start = pIndex;
            int end = sbFileData.indexOf("\n",pIndex);
            if(end<0) break;
            String findResult =findFunction(sbFileData.substring(start,end));
            if(findResult!=null) {
                System.out.println(findResult);
                List<String> lst = confuser.get(fName);
                if(lst==null){
                    lst = new ArrayList<>();
                }
                lst.add(findResult);
                confuser.put(fName,lst);
            }
            pIndex = end+1;
        }
    }

    @Test
    public void test(){
        String table = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String out = "";
        Random random = new Random(System.currentTimeMillis());
        while(out.length()!=table.length()){
            char c = table.charAt(random.nextInt(table.length()));
            if(out.indexOf(c)>-1) continue;
            out += c;
        }
        System.out.println(out);

//        File f = new File("E:\\My_WorkSpace\\Dreaming.Protection\\app\\src\\main\\cpp\\sources\\cJSON.c");
//        findFunctions("utils", readAllLinesForFindFunction(f));

//        System.out.println(encodeInteger(454641));


//        for(int i = 'a';i<='z';++i){
//            System.out.print((char)i);
//        }
//        for(int i = 'A';i<='Z';++i){
//            System.out.print((char)i);
//        }
//        for(int i = '0';i<='9';++i){
//            System.out.print((char)i);
//        }
//        System.out.print("_");
    }
}
