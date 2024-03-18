package com.dreaming.hscj.core.ocr;

import com.baidu.paddle.lite.demo.ocr.OcrResultModel;

import java.util.List;

public interface IRecognizeProcessor<T>{
    T process(List<OcrResultModel> results);
}
