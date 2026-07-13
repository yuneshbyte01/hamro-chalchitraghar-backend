package com.chalchitraghar.modules.tickets.service.impl;

import com.chalchitraghar.modules.tickets.config.TicketQrProperties;
import com.chalchitraghar.modules.tickets.service.QrImageService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class QrImageServiceImpl implements QrImageService {
    private final TicketQrProperties p;

    public QrImageServiceImpl(TicketQrProperties p) {
        this.p = p;
    }

    public byte[] generatePng(String payload) {
        try {
            var matrix =
                    new MultiFormatWriter()
                            .encode(
                                    payload,
                                    BarcodeFormat.QR_CODE,
                                    p.imageSize(),
                                    p.imageSize(),
                                    Map.of(
                                            EncodeHintType.ERROR_CORRECTION,
                                            ErrorCorrectionLevel.H,
                                            EncodeHintType.MARGIN,
                                            p.imageMargin(),
                                            EncodeHintType.CHARACTER_SET,
                                            "UTF-8"));
            var out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate QR image", e);
        }
    }
}
