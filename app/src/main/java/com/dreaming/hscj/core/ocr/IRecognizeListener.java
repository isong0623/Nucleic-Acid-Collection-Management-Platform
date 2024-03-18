package com.dreaming.hscj.core.ocr;

import com.baidu.paddle.lite.demo.ocr.OcrResultModel;

import java.util.List;

public interface IRecognizeListener<T>{
    void onSuccess(T result);
    void onFailure();
    T process(List<OcrResultModel> results);
}
