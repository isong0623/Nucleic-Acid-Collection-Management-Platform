import com.dreaming.hscj.Constants;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.impl.ApiListener;
import com.dreaming.hscj.utils.algorithm.DecryptUtils;
import com.dreaming.hscj.utils.algorithm.EncryptUtils;
import com.dreaming.hscj.utils.algorithm.SignUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import priv.songxusheng.easyjson.ESONArray;
import priv.songxusheng.easyjson.ESONObject;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Test {
    static Logger logger = Logger.getLogger(Test.class.getName());

    /**
     * @see com.dreaming.hscj.utils.algorithm
     */
    static {
        Security.addProvider(new BouncyCastleProvider());
        //windows平台与android平台支持的算法不一致，具体以android为准
        //windows平台与android平台支持的算法不一致，具体以android为准
        //windows平台与android平台支持的算法不一致，具体以android为准
    }

    @org.junit.jupiter.api.Test
    public void doReadTemplateAndTestApi() throws Exception {
        //此处需注意模板文件编码格式为：utf-8
        String path = "C:\\Template.json";
        //此处注意IDE编码格式需为：utf-8
        Template template = Template.read(path);

        Constants.User.setAccount("登录用户名");
        Constants.User.setPassword("登录密码");

        //注意接口返回类型需要根据API返回类型进行判定

        //region 0 登录接口测试
        List<Object> lstParamOfApi0 = new ArrayList<>();//按照ApiParam顺序 必须与param个数一致
        lstParamOfApi0.add(Constants.User.getAccount());
        lstParamOfApi0.add(Constants.User.getPassword());
        StringBuilder sbApi0 = new StringBuilder();
        template.apiOf(0).doRequest(lstParamOfApi0, new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                //在此处配置Token
                ESONObject body = new ESONObject(msg);
//                ESONArray body  = new ESONArray(msg);
                Constants.User.setToken(body.getJSONValue("token",""));
                Constants.User.setExpired(body.getJSONValue("expired",0L));
            }
        },sbApi0);
        logger.info(sbApi0.toString());
        //endregion


        //region 1 获取用户信息接口测试
        List<Object> lstParamOfApi1 = new ArrayList<>();
        StringBuilder sbApi1 = new StringBuilder();
        template.apiOf(1).doRequest(lstParamOfApi1, new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                ESONObject body = new ESONObject(msg);
//                ESONArray body  = new ESONArray(msg);

            }
        },sbApi1);
        logger.info(sbApi1.toString());
        //endregion

        //region 2 获取试管采样记录
        List<Object> lstParamOfApi2 = new ArrayList<>();
        StringBuilder sbApi2= new StringBuilder();
        template.apiOf(2).doRequest(lstParamOfApi2, new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                ESONObject body = new ESONObject(msg);
//                ESONArray body  = new ESONArray(msg);


            }
        },sbApi2);
        logger.info(sbApi2.toString());
        //endregion

        //region 3 获取用户身份信息
        List<Object> lstParamOfApi3 = new ArrayList<>();
        StringBuilder sbApi3= new StringBuilder();
        template.apiOf(3).doRequest(lstParamOfApi3, new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                ESONObject body = new ESONObject(msg);
//                ESONArray body  = new ESONArray(msg);

            }
        },sbApi3);
        logger.info(sbApi3.toString());
        //endregion

        //region 4 提交采样记录
        List<Object> lstParamOfApi4 = new ArrayList<>();
        StringBuilder sbApi4= new StringBuilder();
        template.apiOf(4).doRequest(lstParamOfApi4, new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                ESONObject body = new ESONObject(msg);
//                ESONArray body  = new ESONArray(msg);
            }
        },sbApi4);
        logger.info(sbApi4.toString());
        //endregion

        //region 5 删除采样记录
        List<Object> lstParamOfApi5 = new ArrayList<>();
        StringBuilder sbApi5= new StringBuilder();
        template.apiOf(5).doRequest(lstParamOfApi5, new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                ESONObject body = new ESONObject(msg);
//                ESONArray body  = new ESONArray(msg);


            }
        },sbApi5);
        logger.info(sbApi5.toString());
        //endregion

        //region 6 采样历史查询
        List<Object> lstParamOfApi6 = new ArrayList<>();
        StringBuilder sbApi6= new StringBuilder();
        template.apiOf(6).doRequest(lstParamOfApi6, new ApiListener() {
            @Override
            public void onSuccess(int code, Object msg, StringBuilder sbLog) {
                ESONObject body = new ESONObject(msg);
//                ESONArray body  = new ESONArray(msg);


            }
        },sbApi6);
        logger.info(sbApi6.toString());
    }

    /**
     * @see com.dreaming.hscj.utils.algorithm
     */
    @org.junit.jupiter.api.Test
    public void doAlgorithmTest() throws Exception{
        String encrypted = EncryptUtils.encrypt("encrypt data","secret key","");
        logger.info("encrypted:"+encrypted);
        String decrypted = DecryptUtils.decrypt("decrypt data","secret key", "");
        logger.info("decrypted:"+decrypted);
        String signed    = SignUtils.sign("","","");
        logger.info("signed:"+signed);
    }

    public interface ApiListener extends com.dreaming.hscj.template.api.impl.ApiListener{
        @Override
        default void onPerform() {}

        @Override
        default void onSuccess(int i, Object o, StringBuilder stringBuilder) {}

        @Override
        default void onFailure(int i, Object o, StringBuilder stringBuilder) {
            logger.info("call api error:"+o);
        }

        @Override
        default void onComplete() {}
    }

}
