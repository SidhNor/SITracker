/*
 * Copyright 2016 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui.widget;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import java.util.Arrays;

public class ColoredSpinnerBar extends ProgressBar {

    final int[][] colors = {
            {230, 30, 99},
            {0, 176, 255},
            {48, 63, 159},
            {121, 134, 203}
    };
    RectF rectF;
    Paint p;
    int start = 0;
    int maxvalue = 320;
    int value = 320;
    int[] currentColor = {0, 0, 0};
    boolean reverse = false;
    int nextcolor = 1;

    public ColoredSpinnerBar(Context context) {
        super(context);
        init();
    }

    public ColoredSpinnerBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColoredSpinnerBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        p = new Paint();
        p.setStrokeWidth(6);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.argb(255, colors[0][0], colors[0][1], colors[0][2]));
        currentColor = Arrays.copyOf(colors[0], colors[0].length);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        rectF = new RectF(0 + 5, 0 + 5, w - 5, h - 5);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (reverse)
            start += 15;
        else
            start += 5;

        if (start == 360) {
            start = 1;
        }
        if (!reverse)
            value -= 10;
        else
            value += 10;
        if (value == 0 || value == maxvalue) {
            reverse = !reverse;
        }
        transformColor();
        p.setColor(Color.argb(255, currentColor[0], currentColor[1], currentColor[2]));
        c.drawArc(rectF, start, maxvalue - value, false, p);
        invalidate();
    }

    private void transformColor() {
        changeColors(0);
        changeColors(1);
        changeColors(2);
        if (currentColor[0] == colors[nextcolor][0] && currentColor[1] == colors[nextcolor][1] && currentColor[2] == colors[nextcolor][2]) {
            if (nextcolor == 3)
                nextcolor = 0;
            else
                nextcolor++;
        }
    }

    private void changeColors(int i) {
        if (currentColor[i] > colors[nextcolor][i]) {
            currentColor[i] -= 1;
        }
        if (currentColor[i] < colors[nextcolor][i]) {
            currentColor[i] += 1;
        }
    }
}
