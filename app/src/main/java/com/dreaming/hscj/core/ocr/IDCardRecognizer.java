package com.dreaming.hscj.core.ocr;

import android.media.MediaPlayer;
import android.util.Log;

import com.baidu.paddle.lite.demo.ocr.OcrResultModel;
import com.dreaming.hscj.App;
import com.dreaming.hscj.R;
import com.dreaming.hscj.base.BaseActivity;
import com.dreaming.hscj.template.Template;
import com.dreaming.hscj.template.api.impl.ApiConfig;
import com.dreaming.hscj.utils.CheckUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IDCardRecognizer {

    private static class Card{
        String id;
        String type;
        Card(String id, String type){
            this.id = id;
            this.type = type;
        }
    }
    public interface IDCardRecognizeListener{
        void onSuccess(String id, String type);
        void onFailure();
    }

    public static IRecognizeProcessor<Card> idCardRecognizeProcessor = results -> {
        Map<String,List<String>> mFinder = new HashMap<>();
        ApiConfig.Locate.Card card = Template.getCurrentTemplate().getApiConfig().getCard();
        for(int i=0;i<results.size();++i){
            Log.e("OCRResult",results.get(i).toString());
            String result = results.get(i).getLabel();
            if (result == null) continue;
            StringBuilder sb = new StringBuilder(result);
            String type = CheckUtils.isValidCard(card,sb);
            if(type == null) continue;
            result = sb.toString();
            List<String> lst = mFinder.get(type);
            if(lst == null) lst = new ArrayList<>();
            lst.add(result);
            mFinder.put(type,lst);
        }
        for(int i=0,ni=card.normal.size();i<ni;++i){
            final String type = card.normal.get(i).text;
            List<String> lst = mFinder.get(type);
            if(lst == null || lst.isEmpty()) continue;
            App.Post(()->{
                MediaPlayer mediaPlayer = MediaPlayer.create(App.sInstance,R.raw.id_card_beep);
                mediaPlayer.start();
            });
            return new Card(lst.get(0),type);
        }
        for(int i=0,ni=card.other.size();i<ni;++i){
            final String type = card.other.get(i).getName();
            List<String> lst = mFinder.get(type);
            if(lst == null || lst.isEmpty()) continue;
            App.Post(()->{
                MediaPlayer mediaPlayer = MediaPlayer.create(App.sInstance,R.raw.id_card_beep);
                mediaPlayer.start();
            });
            return new Card(lst.get(0),type);
        }
        return null;
    };

    public static void recognize(BaseActivity activity, IDCardRecognizeListener listener){
        ImageRecognizer.recognize(activity, new IRecognizeListener<Card>() {
            @Override
            public void onSuccess(Card result) {
                listener.onSuccess(result.id,result.type);
            }

            @Override
            public void onFailure() {
                listener.onFailure();
            }

            @Override
            public Card process(List<OcrResultModel> results) {
                return idCardRecognizeProcessor.process(results);
            }
        });
    }
}
