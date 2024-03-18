package com.dreaming.hscj.core;

import com.air4.chinesetts.tts.TtsManager;

public class TTSEngine {

    private static final String TAG = TTSEngine.class.getSimpleName();

    /**
     * 文字转语音并播报
     * @param strChinese
     */
    public static synchronized void speakChinese(final String strChinese){
        TtsManager.getInstance().speak(strChinese, 1, true);
    }

    public static synchronized void speakSync(String text){
        TtsManager.getInstance().speak(text, 1.2f, false);
    }

}
