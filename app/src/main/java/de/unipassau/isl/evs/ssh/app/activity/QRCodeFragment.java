/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.app.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.WriterException;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_MESSAGE;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.QR_CODE_IMAGE_SCALE;

/**
 * QRCodeFragment to display a QR-Code in the app UI. This is used to register new user-devices safely.
 *
 * @author Phil Werli
 */
public class QRCodeFragment extends BoundFragment {
    private static final String KEY_QRCODE_IMAGE = "QRCODE_IMAGE";

    private Serializable extra;

    /**
     * The QR-Code which will be displayed.
     */
    private Bitmap bitmap;

    /**
     * Generates a QR-Code from the sent data.
     *
     * @return the created QR-Code
     */
    private Bitmap createQRCodeBitmap() {
        if (extra instanceof DeviceConnectInformation) {
            try {
                return ((DeviceConnectInformation) extra).toQRBitmap(Bitmap.Config.ARGB_8888, Color.BLACK, Color.WHITE);
            } catch (WriterException e) {
                throw new IllegalArgumentException("illegal QR-Code data", e);
            }
        } else {
            throw new IllegalArgumentException("missing EXTRA_QR_DEVICE_INFORMATION as extra " + EXTRA_QR_DEVICE_INFORMATION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.fragment_qrcode, container, false);
        ImageView imageview = ((ImageView) root.findViewById(R.id.qrcode_fragment_qr_code));

        if (savedInstanceState != null) {
            extra = savedInstanceState.getSerializable(KEY_QRCODE_IMAGE);
        } else if (getArguments() != null) {
            extra = getArguments().getSerializable(EXTRA_QR_DEVICE_INFORMATION);
        }

        if (extra != null) {
            bitmap = createQRCodeBitmap();
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * QR_CODE_IMAGE_SCALE, bitmap.getHeight() * QR_CODE_IMAGE_SCALE, false);
        }

        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
            imageview.setVisibility(View.VISIBLE);
        }
        TextView textView = (TextView) root.findViewById(R.id.qrcode_fragment_text);
        String text = getArguments().getString(EXTRA_QR_MESSAGE, getResources().getString(R.string.please_scan_device));
        textView.setText(text);
        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_QRCODE_IMAGE, extra);
    }
}
