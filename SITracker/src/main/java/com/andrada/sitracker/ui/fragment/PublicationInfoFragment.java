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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.reader.SamlibPublicationPageReader;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.widget.ObservableScrollView;
import com.andrada.sitracker.util.SamlibPageHelper;
import com.andrada.sitracker.util.UIUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.github.kevinsawicki.http.HttpRequest;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.nineoldandroids.view.ViewHelper;
import com.viewpagerindicator.CirclePageIndicator;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

@EFragment(R.layout.fragment_pub_details)
@OptionsMenu(R.menu.publication_info_menu)
public class PublicationInfoFragment extends Fragment implements
        ObservableScrollView.Callbacks {

    private static final float PHOTO_ASPECT_RATIO = 1.7777777f;
    private static final float GAP_FILL_DISTANCE_MULTIPLIER = 1.5f;

    @OrmLiteDao(helper = SiDBHelper.class)
    PublicationDao publicationsDao;

    Publication currentRecord;
    @Pref
    SIPrefs_ prefs;

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
    @ViewById(R.id.pager)
    ViewPager pager;
    @ViewById(R.id.pagerIndicators)
    CirclePageIndicator pagerIndicators;
    @OptionsMenuItem(R.id.action_mark_read)
    MenuItem mMarkAsReadAction;
    private ViewGroup mRootView;
    private Handler mHandler = new Handler();
    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            recomputePhotoAndScrollingMetrics();
        }
    };

    private Uri mPublicationUri;
    private long mPublicationId;
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_pub_details, container, false);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
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

    @Override
    public void onDestroy() {
        OpenHelperManager.releaseHelper();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMarkAsReadAction.setVisible(currentRecord != null && currentRecord.getNew());
    }

    @AfterViews
    public void afterViews() {
        mScrollViewChild.setVisibility(View.INVISIBLE);
        pager.setAdapter(new PublicationImagesAdapter(getActivity()));
        pagerIndicators.setViewPager(pager);
        pagerIndicators.setSnap(true);
        setupCustomScrolling();
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

        String imagesUrl = currentRecord.getImagePageUrl();
        if (!TextUtils.isEmpty(imagesUrl) && prefs.displayPubImages().get() &&
                //TODO remove Gingerbread check on next release
                UIUtils.hasGingerbreadMR1()) {
            //Do a network request to detect number of images
            loadImageList(imagesUrl);
            //Add images
            //Add image view to pager.
            mHasPhoto = true;
            recomputePhotoAndScrollingMetrics();
        } else {
            mHasPhoto = false;
            recomputePhotoAndScrollingMetrics();
        }

        String pubAbstract = SamlibPageHelper.stripDescriptionOfImages(currentRecord.getDescription());
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

    @Background
    void loadImageList(String url) {
        try {
            final HttpRequest request = HttpRequest.get(SamlibPublicationPageReader.SAMLIB_URL_PREFIX + currentRecord.getImagePageUrl());
            //Tolerate 10 days
            request.getConnection().addRequestProperty("Cache-Control", "max-stale=" + (60 * 60 * 24 * 10));
            String data = request.body();
            List<Pair<String, String>> results = new SamlibPublicationPageReader().readPublicationImageUrlsAndDescriptions(data);
            addImagesToList(results);
        } catch (HttpRequest.HttpRequestException e) {
            //TODO log exception to analytics
            addImagesToList(new ArrayList<Pair<String, String>>());
        }
    }

    @UiThread
    void addImagesToList(List<Pair<String, String>> results) {
        if (results.size() == 0) {
            mHasPhoto = false;
        } else {
            if (results.size() == 1) {
                pagerIndicators.setVisibility(View.GONE);
            } else if (results.size() > 15) {
                results = results.subList(0, 15);
            }
            PublicationImagesAdapter adapter = (PublicationImagesAdapter) pager.getAdapter();
            for (Pair<String, String> res : results) {
                adapter.addImage(res.first);
            }
        }
        recomputePhotoAndScrollingMetrics();
    }

    @OptionsItem(R.id.action_mark_read)
    void menuMarkAsReadSelected() {
        try {
            publicationsDao.markPublicationRead(currentRecord);
        } catch (SQLException e) {
            //TODO send analytics exception
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mMarkAsReadAction.setVisible(false);
            }
        });
    }

    @OptionsItem(R.id.open_pub_in_browser)
    void menuOpenInBrowserSelected() {
        if (currentRecord != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(currentRecord.getUrl()));
            getActivity().startActivity(intent);
        }
    }

    private void setupCustomScrolling() {
        mScrollView.addCallbacks(this);
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
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
            mPhotoHeightPixels = (int) (mPhotoViewContainer.getWidth() / PHOTO_ASPECT_RATIO);
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

        if (UIUtils.hasHoneycombMR1()) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)
                    mDetailsContainer.getLayoutParams();
            if (mlp.topMargin != mHeaderHeightPixels + mPhotoHeightPixels) {
                mlp.topMargin = mHeaderHeightPixels + mPhotoHeightPixels;
                mDetailsContainer.setLayoutParams(mlp);
            }
        } else {
            //Set paddings instead
            int paddTop = mDetailsContainer.getPaddingTop();
            if (paddTop != mHeaderHeightPixels + mPhotoHeightPixels + 16) {
                mDetailsContainer.setPadding(mDetailsContainer.getPaddingLeft(),
                        mHeaderHeightPixels + mPhotoHeightPixels + 16,
                        mDetailsContainer.getPaddingRight(),
                        mDetailsContainer.getPaddingBottom());
            }

        }

        onScrollChanged(0, 0); // trigger scroll handling
    }


    class PublicationImagesAdapter extends PagerAdapter {

        Context context;
        List<ImageView> mImages;

        public PublicationImagesAdapter(Context context) {
            this.context = context;
            mImages = new ArrayList<ImageView>();
        }

        public void addImage(String url) {
            final ImageView img = new ImageView(context);
            img.setImageResource(R.drawable.glyph_folder_white);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mImages.add(img);

            int width = 320;
            int height = (int) (320 / PHOTO_ASPECT_RATIO);

            if (mRootView.getWidth() != 0 && mPhotoHeightPixels != 0) {
                width = mRootView.getWidth();
                height = mPhotoHeightPixels;
            }
            if (getActivity() != null) {
                Glide.with(getActivity())
                        .load(url)
                        .fitCenter()
                        .into(new SimpleTarget<GlideDrawable>(width, height) {
                            @Override
                            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                                img.setImageDrawable(resource);
                                // Trigger image transition
                                recomputePhotoAndScrollingMetrics();
                            }
                        });
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mImages.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ImageView v = mImages.get(position);
            container.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
