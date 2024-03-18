package com.dreaming.hscj;

import com.dreaming.hscj.core.TTSEngine;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.utils.CheckUtils;
import com.dreaming.hscj.utils.ExcelUtils;
import com.dreaming.hscj.utils.FileUtils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.HttpHeaders;
import com.tencent.bugly.crashreport.CrashReport;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import okhttp3.Response;
import priv.songxusheng.easyjson.ESON;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void testExcel(){
        String b = "Database";
        String str = "    private int type;\n" +
                "    private String description;\n" +
                "    private String password;\n" +
                "    private List<ApiParam> fields;\n" +
                "    private Map<String,List<String>> setter;\n" +
                "    private Map<Integer,Set<String>> setMapper;\n" +
                "    private Map<String,List<String>> getter;\n" +
                "    private Map<Integer,Set<String>> getMapper;";

        String splits[] = str.split(";");
        StringBuilder sb = new StringBuilder();
        for(String s:splits){
            s = s.trim();
            String split[] = s.split(" ");
            String uppercase = split[2].substring(0,1).toUpperCase()+split[2].substring(1);
            sb.append(s).append(";\n");
            sb.append(String.format("public %s set%s(%s %s){",b,uppercase,split[1],split[2])).append("\n");
            sb.append(String.format("this.%s = %s;",split[2],split[2])).append("\n");
            sb.append(String.format("return this;")).append("\n");
            sb.append(String.format("}")).append("\n");
            sb.append(String.format("public %s get%s(){",split[1],uppercase)).append("\n");
            sb.append(String.format("return %s;",split[2])).append("\n");
            sb.append(String.format("}")).append("\n");
            sb.append("\n");
        }
        System.out.println(sb);
    }

    Runtime run = Runtime.getRuntime();
    String webroot = "E:\\My_Developments\\ffmpeg\\bin\\";
    void convertMp3ToMAV(String src,String dst){
        try {
            long start=System.currentTimeMillis();

            Process p=run.exec(String.format("%sffmpeg.exe -i %s -ar 16000 -acodec pcm_s16le %s",webroot,src,dst));
            p.getOutputStream().close();
            p.getInputStream().close();
            p.getErrorStream().close();
            p.waitFor();
            long end=System.currentTimeMillis();
            System.out.println("convert success, costs:"+(end-start)+"ms "+src);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void convert(File f){
        if(f == null) return;
        if(f.isDirectory()){
            File fs[] = f.listFiles();
            if(fs==null) return;
            for(File f0:fs){
                convert(f0);
            }
            return;
        }
        String dst = f.getParentFile().getParentFile().getParentFile().getAbsolutePath();
        dst = dst + "/tts/chinese/" +f.getParentFile().getName()+"/"+(f.getName().replaceAll(".mp3",""))+".wav";
        if(!new File(dst).getParentFile().exists())new File(dst).getParentFile().mkdirs();
        convertMp3ToMAV(f.getAbsolutePath(), dst);
    }


    String toTimeString(int time){
        int hour = time/3600000;
        time = time%3600000;

        int minute = time/60000;
        time = time%60000;

        int second = time/1000;

        int mills = time%1000;

        return String.format("%02d:%02d:%02d.%03d",hour,minute,second,mills);
    }

    @Test
    public void cutAlphabet26(){
        //                       A   B    C    D    E    F    G    H    I     J     K     L     M     N     O     P     Q     R     S     T     U     V     W     X     Y      Z
        int starts[] = new int[]{296,1370,2470,3737,5065,6263,7384,8655, 9893,11011,12312,14572,15781,17042,18315,19538,20811,22129,23463,24700,25967,27165,28398,29819,30981,33489};
        int ends  [] = new int[]{801,1993,3155,4422,5667,6826,8046,9341,10454,11671,12894,15085,16299,17574,18801,20108,21439,22646,24031,25283,26553,27745,29183,30502,31605,34147};
        for(int i=0;i<starts.length;++i){
//            System.out.println(((char)('A'+i)) + " " + toTimeString(starts[i])+" "+toTimeString(ends[i]-starts[i]) + " "+ toTimeString(ends[i]));
            System.out.println("\""+((char)('A'+i)) + "\":" + (ends[i] - starts[i])+",");
        }

        if(true) return;
        String path = "C:\\Users\\Isidore\\Desktop\\alphabet.wav";
        for(int i=0;i<starts.length;++i){
            String dst = new File(path).getParentFile().getAbsolutePath()+ "/tts/alphabet/" +((char)(('A'+i)))+".wav";
            File f = new File(dst);
            if(!f.getParentFile().exists()) f.getParentFile().mkdirs();
            try {
                long start=System.currentTimeMillis();

                Process p=run.exec(
                        String.format("%sffmpeg.exe -i %s -vn -acodec copy -ss %s -t %s %s",
                                webroot,
                                path,
                                toTimeString(starts[i]),
                                toTimeString(ends[i]-starts[i]),
                                dst
                        )
                );
                p.getOutputStream().close();
                p.getInputStream().close();
                p.getErrorStream().close();
                p.waitFor();
                long end=System.currentTimeMillis();
                System.out.println("cut success, costs:"+(end-start)+"ms from "+toTimeString(starts[i])+" to "+toTimeString(ends[i])+" "+dst);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void convertMp3ToWav(){
        String path = "C:\\Users\\Isidore\\Desktop\\chinese";
        File fRoot = new File(path);
        convert(fRoot);
    }

    void doTerm(String type, String names [], int starts[], int ends[]){
        for(int i=0;i<names.length;++i){
            System.out.println("\""+names[i]+"\":"+(ends[i]-starts[i])+",");
        }

        if(true) return;
        String inputRoot  = new File("src/main/assets/chinese/"+type+"/").getAbsolutePath()+"\\";
        String outputRoot = "C:\\Users\\Isidore\\Desktop\\chinese\\"+type+"\\";
        for(int i=0;i<starts.length;++i){
            String input = inputRoot + names[i]+".wav";
            String output = outputRoot + names[i]+".wav";
            File f = new File(output);
            if(!f.getParentFile().exists()) f.getParentFile().mkdirs();
            try {
                long start=System.currentTimeMillis();

                Process p=run.exec(
                        String.format("%sffmpeg.exe -i %s -vn -acodec copy -ss %s -t %s %s",
                                webroot,
                                input,
                                toTimeString(starts[i]),
                                toTimeString(ends[i]-starts[i]),
                                output
                        )
                );
                p.getOutputStream().close();
                p.getInputStream().close();
                p.getErrorStream().close();
                p.waitFor();
                long end=System.currentTimeMillis();
                System.out.println("cut success, costs:"+(end-start)+"ms from "+toTimeString(starts[i])+" to "+toTimeString(ends[i])+" "+output);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Test
    public void termChinese(){//after convertMp3ToWav
        String initialsNames [] = new String[]{"b","c","ch","d","f","g","h","j","k","l","m","n","p","q","r","s","sh","t","w","x","y","z","zh"};
        int    initialsStarts[] = new int   []{326,321, 116,274,200,133,195,208,255,186,219,179,129,179,125,175,116 ,173,116,174,238,214, 185};
        int    initialsEnds  [] = new int   []{812,894, 722,757,995,701,863,823,806,745,847,798,763,788,751,826,836 ,778,707,889,812,818, 776};
        doTerm("initials",initialsNames,initialsStarts,initialsEnds);

        String vowelsNames1  [] = new String[]{"a1","a2","a3","a4","ai1","ai2","ai3","ai4","an1","an2","an3","an4","ang1","ang2","ang3","ang4","ao1","ao2","ao3","ao4","bo1","bo2","bo3","bo4"};
        int    vowelsStarts1 [] = new int   []{ 284, 306, 305, 294,  124,  196,  138, 148,   196,  148,  155,  121,   117,   156,   113,   101,  160,  131,  188,   99,  349,  222,  249,  212};
        int    vowelsEnds1   [] = new int   []{ 710, 773, 899, 597,  531,  699,  770, 534,   557,  546,  763,  461,   594,   728,   798,   528,  665,  715,  883,  546,  820,  815,  892,  581};
        doTerm("vowels",vowelsNames1,vowelsStarts1,vowelsEnds1);

        String vowelsNames2  [] = new String[]{"chi1","chi2","chi3","chi4","ci1","ci2","ci3","ci4","de1","de2","de3","de4","e1","e2","e3","e4","ei1","ei2","ei3","ei4","en1","en2","en3","en4"};
        int    vowelsStarts2 [] = new int   []{   113,   226,   134,   186,  356,  371,  228,  300,  275,  178,  208,  173, 274, 382, 293, 238,  211,  156,  106,  107,  141,  189,  285,  167};
        int    vowelsEnds2   [] = new int   []{   701,  1023,  1158,   655,  886, 1017, 1200,  716,  792,  813, 1026,  594, 764, 976,1068, 702,  707,  676,  772,  493,  580,  626,  908,  570};
        doTerm("vowels",vowelsNames2,vowelsStarts2,vowelsEnds2);

        String vowelsNames3  [] = new String[]{"eng1","eng2","eng3","eng4","er1","er2","er3","er4","fo1","fo2","fo3","fo4","ge1","ge2","ge3","ge4","he1","he2","he3","he4","i1","i2","i3","i4"};
        int    vowelsStarts3 [] = new int   []{   214,   167,   173,    91,  195,  171,  158,  178,  200,  152,  168,  163,  132,  182,  112,  119,  208,  308,  200,  231, 295, 343, 278, 338};
        int    vowelsEnds3   [] = new int   []{   748,   752,   849,   499,  659,  807,  833,  605,  997, 1021, 1140,  675,  703,  797,  842,  543,  864, 1105, 1136,  738, 845, 938,1116, 801};
        doTerm("vowels",vowelsNames3,vowelsStarts3,vowelsEnds3);

        String vowelsNames4  [] = new String[]{"ie1","ie2","ie3","ie4","in1","in2","in3","in4","ing1","ing2","ing3","ing4","iu1","iu2","iu3","iu4","ji1","ji2","ji3","ji4","ke1","ke2","ke3","ke4"};
        int    vowelsStarts4 [] = new int   []{  146,  101,  157,  174,  159,  233,  260,  178,   181,   155,   141,   129,  137,  138,  138,  162,  207,  204,  277,  257,  259,  268,  135,  183};
        int    vowelsEnds4   [] = new int   []{  707,  830,  964,  618,  755,  864, 1109,  550,   846,   850,  1091,   584,  748,  870,  861,  680,  752,  856, 1137,  646,  779, 1005, 1041,  681};
        doTerm("vowels",vowelsNames4,vowelsStarts4,vowelsEnds4);

        String vowelsNames5  [] = new String[]{"le1","le2","le3","le4","mo1","mo2","mo3","mo4","ne1","ne2","ne3","ne4","o1","o2","o3","o4","ong1","ong2","ong3","ong4","ou1","ou2","ou3","ou4"};
        int    vowelsStarts5 [] = new int   []{  189,  158,  266,  250,  222,  259,  285,  162,  184,  209,  184,  173, 397, 419, 253, 285,   124,   120,   213,   136,  185,  164,  139,  168};
        int    vowelsEnds5   [] = new int   []{  723,  836,  998,  713,  840, 1020, 1147,  630,  783,  948, 1034,  617, 913, 979,1005, 588,   692,   744,   980,   515,  617,  689,  768,  568};
        doTerm("vowels",vowelsNames5,vowelsStarts5,vowelsEnds5);

        String vowelsNames6  [] = new String[]{"po1","po2","po3","po4","qi1","qi2","qi3","qi4","ri1","ri2","ri3","ri4","shi1","shi2","shi3","shi4","si1","si2","si3","si4","te1","te2","te3","te4"};
        int    vowelsStarts6 [] = new int   []{  127,  178,  146,  150,  185,  127,  178,  236,  128,  174,  166,  152,   118,   136,   149,   211,  174,  145,  170,  102,  176,  245,  130,  125};
        int    vowelsEnds6   [] = new int   []{  793, 1002,  990,  650,  787,  874, 1129,  792,  745,  872, 1047,  633,   826,   988,  1158,   729,  812,  919, 1090,  592,  752, 1000,  945,  645};
        doTerm("vowels",vowelsNames6,vowelsStarts6,vowelsEnds6);

        String vowelsNames7  [] = new String[]{"u1","u2","u3","u4","ui1","ui2","ui3","ui4","un1","un2","un3","un4","v1","v2","v3","v4","ve1","ve2","ve3","ve4","vn1","vn2","vn3","vn4","wu1","wu2","wu3","wu4"};
        int    vowelsStarts7 [] = new int   []{ 295, 318, 248, 354,  174,  138,  157,  225,  160,  217,  133,  180, 261, 315, 292, 254,  180,  172,  157,  147,  140,  249,  278,  169,  118,  124,  197,   92};
        int    vowelsEnds7   [] = new int   []{ 794, 891,1060, 734,  753,  791,  898,  614,  671,  754,  776,  552, 774, 864,1057, 631,  725,  830,  925,  501,  806,  935, 1120,  512,  662,  811,  978,  516};
        doTerm("vowels",vowelsNames7,vowelsStarts7,vowelsEnds7);

        String vowelsNames8  [] = new String[]{"xi1","xi2","xi3","xi4","ye1","ye2","ye3","ye4","yi1","yi2","yi3","yi4","yin1","yin2","yin3","yin4","ying1","ying2","ying3","ying4","yu1","yu2","yu3","yu4"};
        int    vowelsStarts8 [] = new int   []{  182,  160,  108,  113,  168,  214,  263,  243,  241,  143,  110,  175,   118,   202,   257,    138,   163,    116,    145,    178,  161,  281,  208,  134};
        int    vowelsEnds8   [] = new int   []{  871,  934, 1111,  618,  701,  877,  976,  648,  806,  724,  929,  586,   581,   787,  1024,    580,   838,    795,    974,    592,  658,  952, 1057,  532};
        doTerm("vowels",vowelsNames8,vowelsStarts8,vowelsEnds8);

        String vowelsNames9  [] = new String[]{"yuan1","yuan2","yuan3","yuan4","yue1","yue2","yue3","yue4","yun1","yun2","yun3","yun4","zhi1","zhi2","zhi3","zhi4","zi1","zi2","zi3","zi4"};
        int    vowelsStarts9 [] = new int   []{    279,    237,    184,    195,   117,   139,   155,   107,   156,   223,   135,   175,   185,   161,   168,   164,  226,  172,  271,  114};
        int    vowelsEnds9   [] = new int   []{    904,    952,    959,    657,   653,   797,   905,   496,   772,   906,   912,   566,   769,   777,  1020,   547,  824,  746, 1077,  499};
        doTerm("vowels",vowelsNames9,vowelsStarts9,vowelsEnds9);
    }

    private String nextPassword(int num){
        StringBuilder sb = new StringBuilder();
        Random random = new Random(System.currentTimeMillis());
        String password = "0123456789aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ~`!@#$%^&*()_+=-[]\\|}{;':\"<>?,./";
        for(int i=0;i<num;++i){
            sb.append(password.charAt(random.nextInt(password.length())));
        }

        return sb.toString();
    }

    @Test
    public void zipHelpHtml() throws Exception{
        String sHtmlPath   = "C:\\Users\\Isidore\\Desktop\\app_help.html";
        String sResPath    = "C:\\Users\\Isidore\\Desktop\\app_help.files";
        String zipSavePath = "C:\\Users\\Isidore\\Desktop\\app_help.zip";
        String password    = "*aR[fS(wEy552!gCVL!TM?$.d[w4jQ;6<@Hw!hp[as{<jd3<x,}!J29:Ub#gB\\T<iU1t>tb]AHv6jN'\"g;_-5W!@oN{`~A42?Bd,amfoF=z)V'jO41f2heHph%qa:eUI(se8\\b<+8$s+'%)ki?zKFCG4{.SI(OLR+(jq%0Df9LP}}l{6B:Pz+$eoo2kX`(*z\"57`qp6,CX^c,3P$|=l1S1Y^L('ErZ|njwGG/4EP%{kOy[!Egz+l\\\\;[09wJ\\QI?";

        System.out.println(password);

        File fZip = new File(zipSavePath);
        if(fZip.exists()) fZip.delete();
        else if(!fZip.getParentFile().exists())fZip.getParentFile().mkdirs();

        ZipFile zip = new ZipFile(new File(zipSavePath),password.toCharArray());
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
        zip.addFile(new File(sHtmlPath),zipParameters);
        zip.addFolder(new File(sResPath),zipParameters);
        zip.close();

        System.out.println("done!");
    }

    @Test
    public void TestZip() throws Exception{
        String password    = "*aR[fS(wEy552!gCVL!TM?$.d[w4jQ;6<@Hw!hp[as{<jd3<x,}!J29:Ub#gB\\T<iU1t>tb]AHv6jN'\"g;_-5W!@oN{`~A42?Bd,amfoF=z)V'jO41f2heHph%qa:eUI(se8\\b<+8$s+'%)ki?zKFCG4{.SI(OLR+(jq%0Df9LP}}l{6B:Pz+$eoo2kX`(*z\"57`qp6,CX^c,3P$|=l1S1Y^L('ErZ|njwGG/4EP%{kOy[!Egz+l\\\\;[09wJ\\QI?";

        File fZip = new File("C:\\Users\\Isidore\\Desktop\\test.zip");
        ZipFile zip = new ZipFile(fZip,password.toCharArray());
        zip.addFile("C:\\Users\\Isidore\\Desktop\\ic_menu_qr_generate.png");
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
        zip.addFile("C:\\Users\\Isidore\\Desktop\\青岛核酸采集浏览器辅助插件v1.3.crx",zipParameters);
        zip.close();
    }

    @Test
    public void TestBugly(){
        StringBuilder sb = new StringBuilder();

        CrashReport.postCatchedException(new Throwable(nextPassword(6553600)));

    }
}