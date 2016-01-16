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

package com.andrada.sitracker.ui.transition;
import android.animation.Animator;
import android.annotation.TargetApi;
import android.graphics.Point;
import android.os.Build;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RevealTransition extends Visibility {
    private final Point mEpicenter;
    private final int mSmallRadius;
    private final int mBigRadius;
    private final long mDuration;

    public RevealTransition(Point epicenter, int smallRadius, int bigRadius, long duration) {
        mEpicenter = epicenter;
        mSmallRadius = smallRadius;
        mBigRadius = bigRadius;
        mDuration = duration;
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        Animator animator = ViewAnimationUtils.createCircularReveal(view, mEpicenter.x, mEpicenter.y,
                mSmallRadius, mBigRadius);
        animator.setDuration(mDuration);
        return new WrapperAnimator(animator);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        Animator animator = ViewAnimationUtils.createCircularReveal(view, mEpicenter.x, mEpicenter.y,
                mBigRadius, mSmallRadius);
        animator.setDuration(mDuration);
        return new WrapperAnimator(animator);
    }
}