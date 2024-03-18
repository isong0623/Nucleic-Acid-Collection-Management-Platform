package com.dreaming.hscj.utils;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.dreaming.hscj.Constants;
import com.dreaming.hscj.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.aztec.AztecWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.maxicode.MaxiCodeReader;
import com.google.zxing.oned.CodaBarWriter;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.Code39Writer;
import com.google.zxing.oned.Code93Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.oned.EAN8Writer;
import com.google.zxing.oned.ITFWriter;
import com.google.zxing.oned.OneDimensionalCodeWriter;
import com.google.zxing.oned.UPCAWriter;
import com.google.zxing.oned.UPCEANWriter;
import com.google.zxing.oned.UPCEWriter;
import com.google.zxing.oned.rss.RSS14Reader;
import com.google.zxing.oned.rss.expanded.RSSExpandedReader;
import com.google.zxing.pdf417.PDF417Writer;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Hashtable;

public class ZxingUtils {
    /**
     * 生成简单二维码
     *
     * @param content                字符串内容
     * @param width                  二维码宽度
     * @param height                 二维码高度
     * @param character_set          编码方式（一般使用UTF-8）
     * @param error_correction_level 容错率 L：7% M：15% Q：25% H：35%
     * @param margin                 空白边距（二维码与边框的空白区域）
     * @param color_black            黑色色块
     * @param color_white            白色色块
     * @return BitMap
     */
    public static Bitmap autoCreateCodeBitmap(String content, int width, int height,
                                              String character_set, String error_correction_level,
                                              String margin, int color_black, int color_white) {

        return createBarcodeBitmap(content,width,height,character_set,error_correction_level,margin,color_black,color_white,BarcodeFormat.values()[Constants.User.getDefaultCodeDisplayStyle(BarcodeFormat.QR_CODE.ordinal()%BarcodeFormat.values().length)]);
    }

    public static Bitmap createBarcodeBitmap(String content, int width, int height,
                                            String character_set, String error_correction_level,
                                            String margin, int color_black, int color_white,
                                            BarcodeFormat format) {
        // 字符串内容判空
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        // 宽和高>=0
        if (width < 0 || height < 0) {
            return null;
        }
        try {
            /** 1.设置二维码相关配置 */
            Hashtable<EncodeHintType, String> hints = new Hashtable<>();
            // 字符转码格式设置
            if (!TextUtils.isEmpty(character_set)) {
                hints.put(EncodeHintType.CHARACTER_SET, character_set);
            }
            // 容错率设置
            if (error_correction_level!=null && !TextUtils.isEmpty(error_correction_level)) {
                hints.put(EncodeHintType.ERROR_CORRECTION, error_correction_level);
            }
            // 空白边距设置
            if (margin != null && !TextUtils.isEmpty(margin)) {
                hints.put(EncodeHintType.MARGIN, margin);
            }
            /** 2.将配置参数传入到QRCodeWriter的encode方法生成BitMatrix(位矩阵)对象 */
            BitMatrix bitMatrix = null;

            switch (format){
                case ITF:
                    bitMatrix = new ITFWriter().encode(content, format, width, height, hints);
                    break;
                case AZTEC:
                    bitMatrix = new AztecWriter().encode(content, format, width, height, hints);
                    break;
                case EAN_8:
                    bitMatrix = new EAN8Writer().encode(content, format, width, height, hints);
                    break;
                case UPC_A:
                    bitMatrix = new UPCAWriter().encode(content, format, width, height, hints);
                    break;
                case UPC_E:
                    bitMatrix = new UPCEWriter().encode(content, format, width, height, hints);
                    break;
                case EAN_13:
                    bitMatrix = new EAN13Writer().encode(content, format, width, height, hints);
                    break;
                case CODE_39:
                    bitMatrix = new Code39Writer().encode(content, format, width, height, hints);
                    break;
                case CODE_93:
                    bitMatrix = new Code93Writer().encode(content, format, width, height, hints);
                    break;
                case PDF_417:
                    bitMatrix = new PDF417Writer().encode(content, format, width, height, hints);
                    break;
                case QR_CODE:
                    bitMatrix = new QRCodeWriter().encode(content, format, width, height, hints);
                    break;
                case CODE_128:
                    bitMatrix = new Code128Writer().encode(content, format, width, height, hints);
                    break;
                case DATA_MATRIX:
                    bitMatrix = new DataMatrixWriter().encode(content, format, width, height, hints);
                    break;
                case CODABAR:
                    bitMatrix = new CodaBarWriter().encode(content, format, width, height, hints);
                    break;
                case RSS_EXPANDED:
                case UPC_EAN_EXTENSION:
                case MAXICODE:
                case RSS_14:
                    break;
            }

            /** 3.创建像素数组,并根据BitMatrix(位矩阵)对象为数组元素赋颜色值 */
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    //bitMatrix.get(x,y)方法返回true是黑色色块，false是白色色块
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = color_black;//黑色色块像素设置
                    } else {
                        pixels[y * width + x] = color_white;// 白色色块像素设置
                    }
                }
            }
            /** 4.创建Bitmap对象,根据像素数组设置Bitmap每个像素点的颜色值,并返回Bitmap对象 */
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

}