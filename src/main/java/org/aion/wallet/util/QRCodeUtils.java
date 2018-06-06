package org.aion.wallet.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.util.HashMap;

/**
 * @author Sergiu-Paul Falcusan
 * -cheers
 */
public class QRCodeUtils {
    private static final int QR_WIDTH = 200;
    private static final int QR_HEIGHT = 200;
    private static final int WHITE = 255 << 16 | 255 << 8 | 255;
    private static final int BLACK = 0;

    public static BufferedImage writeQRCode(String content, int width, int height) {
        BufferedImage image = null;
        try {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); // create an empty image

            HashMap hintMap = new HashMap();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
            hintMap.put(EncodeHintType.MARGIN, 0);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hintMap);


            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    image.setRGB(i, j, bitMatrix.get(i, j) ? BLACK : WHITE); // set pixel one by one
                }
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return image;
    }
}
