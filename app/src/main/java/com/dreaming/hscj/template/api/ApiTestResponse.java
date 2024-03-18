package com.dreaming.hscj.template.api;

import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import priv.songxusheng.easyjson.ESONObject;

import static com.dreaming.hscj.template.api.impl.Api.TYPE_LOGIN;

public class ApiTestResponse {
    //region release

    //region 登录
    public static final String responseOfLogin = "";
    //endregion

    //region 社区信息
    public static final String responseOfGetCommunityInfo = "";
    //endregion

    //region 身份信息
    public static final String responseOfGetPeopleInfo = "";
    //endregion

    //region 提交采样
    public static final String responseOfSamplingNC = "";
    //endregion

    //region 采样查询
    public static final String responseOfSearchingNC = "";
    //endregion

    //region 查询结果
    public static final String responseOfSearchingRecord = "";
    //endregion

    //region 删除采样
    public static final String responseOfDeletingRecord = "";
    //endregion

    //endregion

    //region debug

//    //region 登录
//    public static final String responseOfLogin = "{\n" +
//            "    \"code\": 0,\n" +
//            "    \"msg\": \"登录成功\",\n" +
//            "    \"data\": {\n" +
//            "        \"secondaryId\": null,\n" +
//            "        \"gridName\": \"南墅镇\",\n" +
//            "        \"openId\": null,\n" +
//            "        \"roles\": [\n" +
//            "            \"TEST_ROLE\",\n" +
//            "            \"TOTAL_ROLE\"\n" +
//            "        ],\n" +
//            "        \"regionName\": \"莱西市\",\n" +
//            "        \"siteName\": \"  唐家采集点\",\n" +
//            "        \"delFlag\": false,\n" +
//            "        \"point\": 1,\n" +
//            "        \"regionCode\": \"370285\",\n" +
//            "        \"testSiteId\": \"1355496618522288128\",\n" +
//            "        \"permissionInfos\": [\n" +
//            "            \"grid_api\",\n" +
//            "            \"test_api\",\n" +
//            "            \"site_api\",\n" +
//            "            \"device_api\",\n" +
//            "            \"user_api\",\n" +
//            "            \"test_result_api\",\n" +
//            "            \"people_api\",\n" +
//            "            \"bill_api\",\n" +
//            "            \"test_org_api\",\n" +
//            "            \"grid_api\",\n" +
//            "            \"test_result_api\",\n" +
//            "            \"user_api\",\n" +
//            "            \"people_api\",\n" +
//            "            \"statistics_api\",\n" +
//            "            \"agg_api\"\n" +
//            "        ],\n" +
//            "        \"menus\": [\n" +
//            "            2,\n" +
//            "            8,\n" +
//            "            9,\n" +
//            "            10,\n" +
//            "            14,\n" +
//            "            17,\n" +
//            "            1,\n" +
//            "            2,\n" +
//            "            11,\n" +
//            "            22\n" +
//            "        ],\n" +
//            "        \"siteCode\": \"370285106018\",\n" +
//            "        \"thirdId\": null,\n" +
//            "        \"kind\": 1,\n" +
//            "        \"hashcode\": -1707874779,\n" +
//            "        \"mobile\": \"13153233432\",\n" +
//            "        \"fullName\": \"刘晓玲\",\n" +
//            "        \"isAdmin\": false,\n" +
//            "        \"testOrgId\": null,\n" +
//            "        \"expireAt\": \"2022-03-28 14:47:34\",\n" +
//            "        \"userId\": \"1356050919883776000\",\n" +
//            "        \"token\": \"eyJhbGciOiJIUzI1NiJ9.eyJBdXRob3JpdGllcyI6WyJURVNUX1JPTEUiLCJUT1RBTF9ST0xFIl0sInN1YiI6InRqY3lkIiwia2luZCI6MSwicG9pbnQiOjEsInNpdGVDb2RlIjoiMzcwMjg1MTA2MDE4Iiwic2l0ZU5hbWUiOiLCoMKg5ZSQ5a626YeH6ZuG54K5IiwicmVnaW9uQ29kZSI6IjM3MDI4NSIsInJlZ2lvbk5hbWUiOiLojrHopb_luIIiLCJncmlkSWQiOjE0MSwiZ2lyZE5hbWUiOiLljZflooXplYciLCJtb2JpbGUiOiIxMzE1MzIzMzQzMiIsInRlc3RTaXRlSWQiOjEzNTU0OTY2MTg1MjIyODgxMjgsInVzZXJJZCI6MTM1NjA1MDkxOTg4Mzc3NjAwMCwiaWF0IjoxNjQ4NDMyMDU0LCJleHAiOjE2NDg0NTAwNTR9.LkXxNyFTH74ZhJwV6B9e4Inn6xgH2GLpRVAB2deyxtU\",\n" +
//            "        \"expiration\": null,\n" +
//            "        \"gridId\": \"141\",\n" +
//            "        \"username\": \"tjcyd\"\n" +
//            "    }\n" +
//            "}";
//    //endregion
//
//    //region 社区信息
//    public static final String responseOfGetCommunityInfo = "{\n" +
//            "    \"code\": 0,\n" +
//            "    \"msg\": \"操作成功\",\n" +
//            "    \"data\": {\n" +
//            "        \"regionCode\": \"370285\",\n" +
//            "        \"siteCode\": \"370285106018\",\n" +
//            "        \"gridName\": null,\n" +
//            "        \"kind\": 1,\n" +
//            "        \"regionName\": \"莱西市\",\n" +
//            "        \"hashcode\": -1707874779,\n" +
//            "        \"mobile\": \"13153233432\",\n" +
//            "        \"siteName\": \"  唐家采集点\",\n" +
//            "        \"gridId\": \"141\",\n" +
//            "        \"username\": \"tjcyd\"\n" +
//            "    }\n" +
//            "}";
//    //endregion
//
//    //region 身份信息
//    public static final String responseOfGetPeopleInfo = "{\n" +
//            "    \"code\": 0,\n" +
//            "    \"msg\": \"操作成功\",\n" +
//            "    \"data\": {\n" +
//            "        \"pageNum\": 1,\n" +
//            "        \"pageSize\": 30,\n" +
//            "        \"total\": \"1\",\n" +
//            "        \"pages\": 1,\n" +
//            "        \"orderBy\": null,\n" +
//            "        \"result\": [\n" +
//            "            {\n" +
//            "                \"id\": \"1356820137222782976\",\n" +
//            "                \"fullName\": \"刘进红\",\n" +
//            "                \"idCard\": \"370225196911154721\",\n" +
//            "                \"mobile\": \"18153213412\",\n" +
//            "                \"category\": 1,\n" +
//            "                \"primaryId\": \"141\",\n" +
//            "                \"secondaryId\": \"541\",\n" +
//            "                \"thirdId\": \"1356499526113996800\",\n" +
//            "                \"regionCode\": \"370285\",\n" +
//            "                \"address\": \"南宋2号\",\n" +
//            "                \"remark\": null,\n" +
//            "                \"source\": 0,\n" +
//            "                \"createdTime\": \"2021-02-03 12:21:44\",\n" +
//            "                \"updatedTime\": \"2022-03-20 09:35:02\",\n" +
//            "                \"openId\": null,\n" +
//            "                \"status\": 1,\n" +
//            "                \"delFlag\": false,\n" +
//            "                \"checkStatus\": 1,\n" +
//            "                \"sex\": 0,\n" +
//            "                \"idType\": null,\n" +
//            "                \"createdBy\": null,\n" +
//            "                \"updatedBy\": \"tjcyd\",\n" +
//            "                \"regionName\": null,\n" +
//            "                \"gridName\": \"南墅镇\",\n" +
//            "                \"secondGridName\": \"河南社区村村民委员会\",\n" +
//            "                \"thirdGridName\": \"南宋\",\n" +
//            "                \"gridCode\": null,\n" +
//            "                \"secondGridCode\": null,\n" +
//            "                \"thirdGridCode\": null,\n" +
//            "                \"isNew\": null,\n" +
//            "                \"isPc\": null,\n" +
//            "                \"testNum\": null\n" +
//            "            }\n" +
//            "        ]\n" +
//            "    }\n" +
//            "}";
//    //endregion
//
//    //region 提交采样
//    public static final String responseOfSamplingNC = "{\"code\":0,\"msg\":\"操作成功\",\"data\":{\"testNum\":\"3702851062003953\",\"value\":\"2\"}}";
//    //endregion
//
//    //region 采样查询
//    public static final String responseOfSearchingNC = "{\n" +
//            "    \"code\": 0,\n" +
//            "    \"msg\": \"操作成功\",\n" +
//            "    \"data\": [\n" +
//            "        {\n" +
//            "            \"peopleId\": \"1356826349469409280\",\n" +
//            "            \"fullName\": \"崔振凤\",\n" +
//            "            \"address\": \"水集街道长安人家005号楼一单元301\",\n" +
//            "            \"mobile\": \"17685566953\",\n" +
//            "            \"idCard\": \"370285********3526\",\n" +
//            "            \"testNum\": \"3702851062003953\",\n" +
//            "            \"testSiteName\": \"  唐家采集点\",\n" +
//            "            \"testSiteId\": \"1355496618522288128\",\n" +
//            "            \"testStatus\": 3\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"peopleId\": \"1356820137222782976\",\n" +
//            "            \"fullName\": \"宋旭升\",\n" +
//            "            \"address\": \"南宋村64号\",\n" +
//            "            \"mobile\": \"15254261892\",\n" +
//            "            \"idCard\": \"370285********0810\",\n" +
//            "            \"testNum\": \"3702851062003953\",\n" +
//            "            \"testSiteName\": \"  唐家采集点\",\n" +
//            "            \"testSiteId\": \"1355496618522288128\",\n" +
//            "            \"testStatus\": 3\n" +
//            "        }\n" +
//            "    ]\n" +
//            "}";
//    //endregion
//
//    //region 查询结果
//    public static final String responseOfSearchingRecord = "{\n" +
//            "    \"code\": 0,\n" +
//            "    \"msg\": \"操作成功\",\n" +
//            "    \"data\": {\n" +
//            "        \"pageNum\": 1,\n" +
//            "        \"pageSize\": 30,\n" +
//            "        \"total\": \"2\",\n" +
//            "        \"pages\": 1,\n" +
//            "        \"orderBy\": null,\n" +
//            "        \"result\": [\n" +
//            "            {\n" +
//            "                \"id\": \"1486252803076198778\",\n" +
//            "                \"peopleId\": \"1356820137222782976\",\n" +
//            "                \"fullName\": \"宋旭升\",\n" +
//            "                \"address\": \"南宋村64号\",\n" +
//            "                \"mobile\": \"15254261892\",\n" +
//            "                \"idCard\": \"370285********0810\",\n" +
//            "                \"testNum\": \"3702851062003953\",\n" +
//            "                \"billId\": \"1507169659017928704\",\n" +
//            "                \"gatheringStatus\": 1,\n" +
//            "                \"gatheringTime\": \"2022-03-25 09:23:45\",\n" +
//            "                \"testTime\": null,\n" +
//            "                \"sendTime\": \"2022-03-25 09:37:03\",\n" +
//            "                \"receiveTime\": null,\n" +
//            "                \"testStatus\": 1,\n" +
//            "                \"testResult\": null,\n" +
//            "                \"excludeStatus\": null,\n" +
//            "                \"testOrgId\": \"1482612328326459392\",\n" +
//            "                \"testOrgName\": \"莱西全民核酸检测\",\n" +
//            "                \"testSiteId\": \"1355496618522288128\",\n" +
//            "                \"testSiteName\": \"  唐家采集点\",\n" +
//            "                \"lastGridId\": \"141\",\n" +
//            "                \"regionCode\": \"370285\",\n" +
//            "                \"createdTime\": \"2022-03-25 09:23:45\",\n" +
//            "                \"updatedTime\": null,\n" +
//            "                \"delFlag\": null,\n" +
//            "                \"remark\": null,\n" +
//            "                \"testPlanId\": null,\n" +
//            "                \"machineNum\": null,\n" +
//            "                \"rejectTime\": null,\n" +
//            "                \"finishTime\": null,\n" +
//            "                \"regionName\": \"莱西市\",\n" +
//            "                \"gridName\": \"南墅镇\",\n" +
//            "                \"idType\": null,\n" +
//            "                \"outsidePoint\": null,\n" +
//            "                \"uptosdStatus\": \"1\",\n" +
//            "                \"createdBy\": null,\n" +
//            "                \"updatedBy\": null,\n" +
//            "                \"sex\": null,\n" +
//            "                \"secondGridName\": null,\n" +
//            "                \"thirdGridName\": null,\n" +
//            "                \"category\": null,\n" +
//            "                \"status\": null,\n" +
//            "                \"maxGatheringTime\": null\n" +
//            "            },\n" +
//            "            {\n" +
//            "                \"id\": \"1486252803076169477\",\n" +
//            "                \"peopleId\": \"1356826349469409280\",\n" +
//            "                \"fullName\": \"崔振凤\",\n" +
//            "                \"address\": \"水集街道长安人家005号楼一单元301\",\n" +
//            "                \"mobile\": \"17685566953\",\n" +
//            "                \"idCard\": \"370285********3526\",\n" +
//            "                \"testNum\": \"3702851062003953\",\n" +
//            "                \"billId\": \"1507169659017928704\",\n" +
//            "                \"gatheringStatus\": 1,\n" +
//            "                \"gatheringTime\": \"2022-03-25 09:00:51\",\n" +
//            "                \"testTime\": null,\n" +
//            "                \"sendTime\": \"2022-03-25 09:37:03\",\n" +
//            "                \"receiveTime\": null,\n" +
//            "                \"testStatus\": 1,\n" +
//            "                \"testResult\": null,\n" +
//            "                \"excludeStatus\": null,\n" +
//            "                \"testOrgId\": \"1482612328326459392\",\n" +
//            "                \"testOrgName\": \"莱西全民核酸检测\",\n" +
//            "                \"testSiteId\": \"1355496618522288128\",\n" +
//            "                \"testSiteName\": \"  唐家采集点\",\n" +
//            "                \"lastGridId\": \"141\",\n" +
//            "                \"regionCode\": \"370285\",\n" +
//            "                \"createdTime\": \"2022-03-25 09:00:51\",\n" +
//            "                \"updatedTime\": null,\n" +
//            "                \"delFlag\": null,\n" +
//            "                \"remark\": null,\n" +
//            "                \"testPlanId\": null,\n" +
//            "                \"machineNum\": null,\n" +
//            "                \"rejectTime\": null,\n" +
//            "                \"finishTime\": null,\n" +
//            "                \"regionName\": \"莱西市\",\n" +
//            "                \"gridName\": \"南墅镇\",\n" +
//            "                \"idType\": null,\n" +
//            "                \"outsidePoint\": null,\n" +
//            "                \"uptosdStatus\": \"1\",\n" +
//            "                \"createdBy\": null,\n" +
//            "                \"updatedBy\": null,\n" +
//            "                \"sex\": null,\n" +
//            "                \"secondGridName\": null,\n" +
//            "                \"thirdGridName\": null,\n" +
//            "                \"category\": null,\n" +
//            "                \"status\": null,\n" +
//            "                \"maxGatheringTime\": null\n" +
//            "            }\n" +
//            "        ]\n" +
//            "    }\n" +
//            "}";
//    //endregion
//
//    //region 删除采样
//    public static final String responseOfDeletingRecord = "{\n" +
//            "    \"code\": 0,\n" +
//            "    \"msg\": \"操作成功\"," +
//            "    \"data\": true" +
//            "}";
//    //endregion

    //endregion

    private static final String [] arrBody = new String[]{
            responseOfLogin,
            responseOfGetCommunityInfo,
            responseOfSearchingNC,
            responseOfGetPeopleInfo,
            responseOfSamplingNC,
            responseOfDeletingRecord,
            responseOfSearchingRecord
    };

    public static Response getTestResponse(int type){
        return new Response.Builder()
                .request(new Request.Builder().url("http://www.test.cn/api").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message(arrBody[type])
                .body(ResponseBody.create(MediaType.parse("application/json"),arrBody[type]) )
                .build();
    }
}
