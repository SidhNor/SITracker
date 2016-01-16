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
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class WrapperAnimator extends Animator {
    private final Animator mWrappedAnimator;

    public WrapperAnimator(Animator wrappedAnimator) {
        mWrappedAnimator = wrappedAnimator;
    }

    @Override
    public long getStartDelay() {
        return mWrappedAnimator.getStartDelay();
    }

    @Override
    public void setStartDelay(long startDelay) {
        mWrappedAnimator.setStartDelay(startDelay);
    }

    @Override
    public Animator setDuration(long duration) {
        mWrappedAnimator.setDuration(duration);
        return this;
    }

    @Override
    public long getDuration() {
        return mWrappedAnimator.getDuration();
    }

    @Override
    public void setInterpolator(TimeInterpolator value) {
        mWrappedAnimator.setInterpolator(value);
    }

    @Override
    public boolean isRunning() {
        return mWrappedAnimator.isRunning();
    }

    @Override
    public void start() {
        mWrappedAnimator.start();
    }

    @Override
    public void cancel() {
        mWrappedAnimator.cancel();
    }

    @Override
    public void pause() {
        if (!isRevealAnimator()) {
            mWrappedAnimator.pause();
        } else {
        }
    }

    @Override
    public void resume() {
        if (!isRevealAnimator()) {
            mWrappedAnimator.resume();
        } else {
        }
    }

    @Override
    public void addListener(AnimatorListener listener) {
        mWrappedAnimator.addListener(listener);
    }

    @Override
    public void removeAllListeners() {
        mWrappedAnimator.removeAllListeners();
    }

    @Override
    public void removeListener(AnimatorListener listener) {
        mWrappedAnimator.removeListener(listener);
    }

    private boolean isRevealAnimator() {
        return mWrappedAnimator.getClass().getName().contains("RevealAnimator");
    }
}