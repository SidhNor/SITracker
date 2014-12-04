/*
 * Copyright 2014 Gleb Godonoga.
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

package com.andrada.sitracker.bitmap;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.andrada.sitracker.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

/**
 * A drawable that encapsulates all the functionality needed to display a contact image,
 * including request creation/cancelling and data unbinding/re-binding. While no contact images
 * can be shown, a default letter tile will be shown instead.
 * <p/>
 * <p/>
 */
public class AvatarDrawable extends Drawable {

    private String mCurrentUrl;
    private String mCurrentName;

    private Bitmap mBitmap;
    private final Paint mPaint;

    /**
     * Letter tile
     */
    private static TypedArray sColors;
    private static int sColorCount;
    private static int sDefaultColor;
    private static int sTileLetterFontSize;
    private static int sTileFontColor;
    private static Bitmap DEFAULT_AVATAR;
    /**
     * Reusable components to avoid new allocations
     */
    private static final Paint sPaint = new Paint();
    private static final Rect sRect = new Rect();
    private static final char[] sFirstChar = new char[1];

    private final float mBorderWidth;
    private final Paint mBitmapPaint;
    private final Paint mBorderPaint;
    private final Matrix mMatrix;

    private int mDecodeWidth;
    private int mDecodeHeight;

    public AvatarDrawable(final Resources res) {
        mPaint = new Paint();
        mPaint.setFilterBitmap(true);
        mPaint.setDither(true);

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setFilterBitmap(true);
        mBitmapPaint.setDither(true);

        mBorderWidth = res.getDimensionPixelSize(R.dimen.avatar_border_width);

        mBorderPaint = new Paint();
        mBorderPaint.setColor(Color.TRANSPARENT);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaint.setAntiAlias(true);

        mMatrix = new Matrix();

        if (sColors == null) {
            sColors = res.obtainTypedArray(R.array.letter_tile_colors);
            sColorCount = sColors.length();
            sDefaultColor = res.getColor(R.color.letter_tile_default_color);
            sTileLetterFontSize = res.getDimensionPixelSize(R.dimen.tile_letter_font_size);
            sTileFontColor = res.getColor(R.color.letter_tile_font_color);
            DEFAULT_AVATAR = BitmapFactory.decodeResource(res, R.drawable.avatar_placeholder_gray);

            sPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            sPaint.setTextAlign(Align.CENTER);
            sPaint.setAntiAlias(true);
        }
    }

    @Override
    public void draw(final Canvas canvas) {
        final Rect bounds = getBounds();
        if (!isVisible() || bounds.isEmpty()) {
            return;
        }

        if (mBitmap != null) {
            // Draw sender image.
            drawBitmap(mBitmap, mBitmap.getWidth(), mBitmap.getHeight(), canvas);
        } else {
            // Draw letter tile.
            drawLetterTile(canvas);
        }
    }

    /**
     * Draw the bitmap onto the canvas at the current bounds taking into account the current scale.
     */
    private void drawBitmap(final Bitmap bitmap, final int width, final int height,
                            final Canvas canvas) {
        final Rect bounds = getBounds();
        // Draw bitmap through shader first.
        final BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
        mMatrix.reset();

        // Fit bitmap to bounds.
        final float boundsWidth = (float) bounds.width();
        final float boundsHeight = (float) bounds.height();
        final float scale = Math.max(boundsWidth / width, boundsHeight / height);
        mMatrix.postScale(scale, scale);

        // Translate bitmap to dst bounds.
        mMatrix.postTranslate(bounds.left, bounds.top);

        shader.setLocalMatrix(mMatrix);
        mBitmapPaint.setShader(shader);
        drawCircle(canvas, bounds, mBitmapPaint);

        // Then draw the border.
        final float radius = bounds.width() / 2f - mBorderWidth / 2;
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, mBorderPaint);
    }

    private void drawLetterTile(final Canvas canvas) {
        if (mCurrentName == null) {
            return;
        }

        final Rect bounds = getBounds();

        // Draw background color.
        final String name = mCurrentName;
        sPaint.setColor(pickColor(name));
        sPaint.setAlpha(mPaint.getAlpha());
        drawCircle(canvas, bounds, sPaint);

        // Draw letter/digit or generic avatar.
        final String displayName = mCurrentName;
        final char firstChar = displayName.charAt(0);
        if (isEnglishLetterOrDigit(firstChar) || isRussianLetterOrDigit(firstChar)) {
            // Draw letter or digit.
            sFirstChar[0] = Character.toUpperCase(firstChar);
            sPaint.setTextSize(sTileLetterFontSize);
            sPaint.getTextBounds(sFirstChar, 0, 1, sRect);
            sPaint.setColor(sTileFontColor);
            canvas.drawText(sFirstChar, 0, 1, bounds.centerX(),
                    bounds.centerY() + sRect.height() / 2, sPaint);
        } else {
            drawBitmap(DEFAULT_AVATAR, DEFAULT_AVATAR.getWidth(), DEFAULT_AVATAR.getHeight(),
                    canvas);
        }
    }

    /**
     * Draws the largest circle that fits within the given <code>bounds</code>.
     *
     * @param canvas the canvas on which to draw
     * @param bounds the bounding box of the circle
     * @param paint  the paint with which to draw
     */
    private static void drawCircle(Canvas canvas, Rect bounds, Paint paint) {
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2, paint);
    }

    private static int pickColor(final String imageUrl) {
        // String.hashCode() implementation is not supposed to change across java versions, so
        // this should guarantee the same imageUrl address always maps to the same color.
        // The imageUrl should already have been normalized by the ContactRequest.
        final int color = Math.abs(imageUrl.hashCode()) % sColorCount;
        return sColors.getColor(color, sDefaultColor);
    }

    private static boolean isEnglishLetterOrDigit(final char c) {
        return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || ('0' <= c && c <= '9');
    }


    private static boolean isRussianLetterOrDigit(char c) {
        return 'А' <= c && c <= 'Я' || 'а' <= c && c <= 'я' || '0' <= c && c <= '9';
    }

    @Override
    public void setAlpha(final int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(final ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    public void setDecodeDimensions(final int decodeWidth, final int decodeHeight) {
        mDecodeWidth = decodeWidth;
        mDecodeHeight = decodeHeight;
    }

    public void unbind() {
        setImage(null, null, null);
    }

    public void bind(final Context context, final String name, final String imageUrl) {
        setImage(context, name, imageUrl);
    }

    private void setImage(final Context context, final String name, final String imageUrl) {
        if (mCurrentUrl != null && mCurrentUrl.equals(imageUrl)) {
            return;
        }

        if (mBitmap != null) {
            mBitmap = null;
        }

        mCurrentName = name;
        mCurrentUrl = imageUrl;

        if (mCurrentUrl == null) {
            invalidateSelf();
            return;
        }

        Glide.with(context)
                .load(imageUrl)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(mDecodeWidth, mDecodeHeight) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        setBitmap(resource);
                    }
                });

    }

    private void setBitmap(final Bitmap bmp) {
        mBitmap = bmp;
        invalidateSelf();
    }
}
