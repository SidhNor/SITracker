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

package com.andrada.sitracker.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.widget.ObservableScrollView;
import com.andrada.sitracker.util.ImageLoader;
import com.andrada.sitracker.util.UIUtils;
import com.android.volley.VolleyError;
import com.nineoldandroids.view.ViewHelper;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

@EFragment(R.layout.fragment_pub_details)
public class PublicationInfoFragment extends Fragment implements
        ObservableScrollView.Callbacks {

    private ViewGroup mRootView;

    @OrmLiteDao(helper = SiDBHelper.class, model = Publication.class)
    PublicationDao publicationsDao;

    Publication currentRecord;

    @ViewById(R.id.scroll_view_child)
    View mScrollViewChild;
    @ViewById(R.id.pub_title)
    TextView mTitle;
    @ViewById(R.id.pub_subtitle)
    TextView mSubtitle;
    @ViewById(R.id.scroll_view)
    ObservableScrollView mScrollView;
    @ViewById(R.id.pub_abstract)
    TextView mAbstract;
    @ViewById(R.id.header_pub)
    View mHeaderBox;
    @ViewById(R.id.header_pub_contents)
    View mHeaderContentBox;
    @ViewById(R.id.header_background)
    View mHeaderBackgroundBox;
    @ViewById(R.id.header_shadow)
    View mHeaderShadow;
    @ViewById(R.id.details_container)
    View mDetailsContainer;
    @ViewById(R.id.pub_photo_container)
    View mPhotoViewContainer;
    @ViewById(R.id.pub_photo)
    ImageView mPhotoView;

    ImageLoader mImageLoader;

    private Handler mHandler = new Handler();
    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            //mAddScheduleButtonHeightPixels = mAddScheduleButton.getHeight();
            recomputePhotoAndScrollingMetrics();
        }
    };

    private Uri mPublicationUri;
    private long mPublicationId;

    private static final float PHOTO_ASPECT_RATIO = 1.7777777f;
    private static final float GAP_FILL_DISTANCE_MULTIPLIER = 1.5f;

    private boolean mHasPhoto;
    private boolean mGapFillShown;
    private int mHeaderTopClearance;
    private int mPhotoHeightPixels;
    private int mHeaderHeightPixels;
    //private int mAddScheduleButtonHeightPixels;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        mPublicationUri = intent.getData();

        if (mPublicationUri == null) {
            return;
        }
        mPublicationId = AppUriContract.getPublicationId(mPublicationUri);
        mHandler = new Handler();
        mImageLoader = new ImageLoader(this.getActivity(), R.drawable.blank_book)
                .setFadeInImage(UIUtils.hasHoneycombMR1());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_pub_details, container, false);
        return mRootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mScrollView == null) {
            return;
        }
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            //noinspection deprecation
            vto.removeGlobalOnLayoutListener(mGlobalLayoutListener);
        }
    }

    @AfterViews
    public void afterViews() {
        mScrollViewChild.setVisibility(View.INVISIBLE);
        setupCustomScrolling();
    }


    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Background
    void loadData() {
        currentRecord = publicationsDao.getPublicationForId(mPublicationId);
        bindData();
    }

    @UiThread
    void bindData() {
        String mTitleString = currentRecord.getName();
        String subtitle = currentRecord.getCategory();

        mTitle.setText(mTitleString);
        mSubtitle.setText(subtitle);

        mPhotoViewContainer.setBackgroundColor(UIUtils.scaleColor(0xe8552c, 0.65f, false));

        String photo = currentRecord.getImageUrl();
        if (!TextUtils.isEmpty(photo)) {
            mHasPhoto = true;
            mImageLoader.get(photo, new com.android.volley.toolbox.ImageLoader.ImageListener() {
                @Override
                public void onResponse(com.android.volley.toolbox.ImageLoader.ImageContainer response, boolean isImmediate) {
                    mPhotoView.setImageBitmap(response.getBitmap());
                    // Trigger image transition
                    recomputePhotoAndScrollingMetrics();
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    mHasPhoto = false;
                    recomputePhotoAndScrollingMetrics();
                }
            }, mRootView.getWidth(), mPhotoHeightPixels);
            recomputePhotoAndScrollingMetrics();
        } else {
            mHasPhoto = false;
            recomputePhotoAndScrollingMetrics();
        }

        String pubAbstract = currentRecord.getDescription();
        if (!TextUtils.isEmpty(pubAbstract)) {
            UIUtils.setTextMaybeHtml(mAbstract, pubAbstract);
            mAbstract.setVisibility(View.VISIBLE);
        } else {
            mAbstract.setVisibility(View.GONE);
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onScrollChanged(0, 0); // trigger scroll handling
                mScrollViewChild.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupCustomScrolling() {
        mScrollView.addCallbacks(this);
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
        }
    }

    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        final BaseActivity activity = (BaseActivity) getActivity();
        if (activity == null) {
            return;
        }

        // Reposition the header bar -- it's normally anchored to the top of the content,
        // but locks to the top of the screen on scroll
        int scrollY = mScrollView.getScrollY();

        float newTop = Math.max(mPhotoHeightPixels, scrollY + mHeaderTopClearance);

        if (UIUtils.hasHoneycombMR1()) {
            mHeaderBox.setTranslationY(newTop);
            /*mAddScheduleButton.setTranslationY(newTop + mHeaderHeightPixels
                - mAddScheduleButtonHeightPixels / 2);*/
            mHeaderBackgroundBox.setPivotY(mHeaderHeightPixels);
        } else {
            ViewHelper.setTranslationY(mHeaderBox, newTop);
            ViewHelper.setPivotY(mHeaderBackgroundBox, mHeaderHeightPixels);
        }
        int gapFillDistance = (int) (mHeaderTopClearance * GAP_FILL_DISTANCE_MULTIPLIER);
        boolean showGapFill = !mHasPhoto || (scrollY > (mPhotoHeightPixels - gapFillDistance));
        float desiredHeaderScaleY = showGapFill ?
                ((mHeaderHeightPixels + gapFillDistance + 1) * 1f / mHeaderHeightPixels)
                : 1f;

        if (!mHasPhoto) {
            if (UIUtils.hasHoneycombMR1()) {
                mHeaderBackgroundBox.setScaleY(desiredHeaderScaleY);
            } else {
                ViewHelper.setScaleY(mHeaderBackgroundBox, desiredHeaderScaleY);
            }
        } else if (mGapFillShown != showGapFill) {
            if (UIUtils.hasICS()) {
                mHeaderBackgroundBox.animate()
                        .scaleY(desiredHeaderScaleY)
                        .setInterpolator(new DecelerateInterpolator(2f))
                        .setDuration(250)
                        .start();
            } else {
                animate(mHeaderBackgroundBox)
                        .scaleY(desiredHeaderScaleY)
                        .setInterpolator(new DecelerateInterpolator(2f))
                        .setDuration(250);
            }
        }
        mGapFillShown = showGapFill;

        mHeaderShadow.setVisibility(View.VISIBLE);

            if (mHeaderTopClearance != 0) {
                // Fill the gap between status bar and header bar with color
                float gapFillProgress = Math.min(Math.max(UIUtils.getProgress(scrollY,
                        mPhotoHeightPixels - mHeaderTopClearance * 2,
                        mPhotoHeightPixels - mHeaderTopClearance), 0), 1);
                if (UIUtils.hasHoneycombMR1()) {
                    mHeaderShadow.setAlpha(gapFillProgress);
                } else {
                    ViewHelper.setAlpha(mHeaderShadow, gapFillProgress);
                }
            }

        // Move background photo (parallax effect)
        if (UIUtils.hasHoneycombMR1()) {
            mPhotoViewContainer.setTranslationY(scrollY * 0.5f);
        } else {
            ViewHelper.setTranslationY(mPhotoViewContainer, scrollY * 0.5f);
        }


    }


    private void recomputePhotoAndScrollingMetrics() {
        final int actionBarSize = UIUtils.calculateActionBarSize(getActivity());
        mHeaderTopClearance = actionBarSize - mHeaderContentBox.getPaddingTop();
        mHeaderHeightPixels = mHeaderContentBox.getHeight();

        mPhotoHeightPixels = mHeaderTopClearance;
        if (mHasPhoto) {
            mPhotoHeightPixels = (int) (mPhotoView.getWidth() / PHOTO_ASPECT_RATIO);
            mPhotoHeightPixels = Math.min(mPhotoHeightPixels, mRootView.getHeight() * 2 / 3);
        }

        ViewGroup.LayoutParams lp;
        lp = mPhotoViewContainer.getLayoutParams();
        if (lp.height != mPhotoHeightPixels) {
            lp.height = mPhotoHeightPixels;
            mPhotoViewContainer.setLayoutParams(lp);
        }

        lp = mHeaderBackgroundBox.getLayoutParams();
        if (lp.height != mHeaderHeightPixels) {
            lp.height = mHeaderHeightPixels;
            mHeaderBackgroundBox.setLayoutParams(lp);
        }

        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)
                mDetailsContainer.getLayoutParams();
        if (mlp.topMargin != mHeaderHeightPixels + mPhotoHeightPixels) {
            mlp.topMargin = mHeaderHeightPixels + mPhotoHeightPixels;
            mDetailsContainer.setLayoutParams(mlp);
        }
        onScrollChanged(0, 0); // trigger scroll handling
    }

}
