# Nucleic-Acid-Collection-Management-Platform
2022青岛全民核酸采集平台安卓版

## 特色功能
模板适配，根据/app/local/hscj的模板适配说明书和工具完成模板适配，此程序可全国通用。

## 主要功能
* 1、成员录入-批量Excel录入，ocr身份证识别，手动录入等
* 2、核酸采样管理-录入（单录，分组批量录）、删除、查询等
* 3、试管转移（平台没有）
* 4、离线采样（平台没有）
* 5、采样数据导出（平台没有）

## 目录结构
```
Nucleic-Acid-Collection-Management-Platform
├─app 
│  ├─local
│  │  ├─hscj #包含模板适配相关说明和工具
│  │  │  ├─hscj_open_source    #用于打包模板适配的jar包，为hscj_template_adapt使用
│  │  │  └─hscj_template_adapt #用于模板适配测试，适配方
│  │  └─release #版本迭代记录
├─libChineseTTS #语音播报，采样时播报
└─libFilePicker #文件选择库
```

##其他说明
* 此程序并不能直接跑，本人将libBarduOcr、libSecurity两个模块保留，如想跑可提取旧版本apk中的相关so库放入项目中
* [具体见微博](https://weibo.com/7711875625/M1Lifgk6g)