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
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
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
import com.andrada.sitracker.exceptions.SharePublicationException;
import com.andrada.sitracker.reader.SamlibPublicationPageReader;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.widget.CheckableFrameLayout;
import com.andrada.sitracker.ui.widget.ObservableScrollView;
import com.andrada.sitracker.util.SamlibPageHelper;
import com.andrada.sitracker.util.ShareHelper;
import com.andrada.sitracker.util.UIUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.kevinsawicki.http.HttpRequest;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.viewpagerindicator.CirclePageIndicator;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
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

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

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
    @ViewById(R.id.read_pub_button)
    CheckableFrameLayout mReadPubButton;
    @OptionsMenuItem(R.id.action_mark_read)
    MenuItem mMarkAsReadAction;
    @OptionsMenuItem(R.id.action_force_download)
    MenuItem mForceDownloadAction;
    @InstanceState
    boolean mIsDownloading = false;

    private boolean mDownloaded;
    private ViewGroup mRootView;
    private Handler mHandler = new Handler();
    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            mReadPubButtonHeightPixels = mReadPubButton.getHeight();
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
    private int mReadPubButtonHeightPixels;


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
        if (mIsDownloading) {
            MenuItemCompat.setActionView(mForceDownloadAction, R.layout.ab_download_progress);
        }
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

    @Background
    void forceDownloadPublication() {
        int errorMessage = -1;
        try {
            //We are not interested in the actual result intent.
            ShareHelper.fetchPublication(getActivity(), currentRecord, prefs.downloadFolder().get(), true);
        } catch (SharePublicationException e) {
            switch (e.getError()) {
                case COULD_NOT_PERSIST:
                    errorMessage = R.string.publication_error_save;
                    break;
                case STORAGE_NOT_ACCESSIBLE_FOR_PERSISTANCE:
                    errorMessage = R.string.publication_error_storage;
                    break;
                case ERROR_UNKOWN:
                    errorMessage = R.string.publication_error_unknown;
                    break;
                case COULD_NOT_LOAD:
                    errorMessage = R.string.cannot_download_publication;
                    break;
                case WRONG_PUBLICATION_URL:
                    errorMessage = R.string.publication_error_url;
                    break;
                default:
                    break;
            }
            final String msg = getResources().getString(errorMessage);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Style.Builder alertStyle = new Style.Builder()
                            .setTextAppearance(android.R.attr.textAppearanceLarge)
                            .setPaddingInPixels(25);
                    alertStyle.setBackgroundColorValue(Style.holoRedLight);
                    Crouton.makeText(getActivity(), msg, alertStyle.build()).show();
                }
            });
        } finally {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MenuItemCompat.setActionView(mForceDownloadAction, null);
                }
            });
        }
    }

    @UiThread
    void bindData() {
        String mTitleString = currentRecord.getName();
        String subtitle = currentRecord.getCategory();

        mTitle.setText(mTitleString);
        mSubtitle.setText(subtitle);

        mReadPubButton.setVisibility(View.VISIBLE);
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
            mAbstract.setMovementMethod(LinkMovementMethod.getInstance());
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

    @OptionsItem(R.id.action_force_download)
    void menuForceDownloadSelected() {
        if (currentRecord != null && !mIsDownloading) {
            mIsDownloading = true;
            //Change action view
            MenuItemCompat.setActionView(mForceDownloadAction, R.layout.ab_download_progress);
            //Start downloading in a background thread
            forceDownloadPublication();
        }
    }

    @Click(R.id.read_pub_button)
    void downloadAndReadButtonClicked() {
        boolean downloaded = !mDownloaded;
        showDownloaded(downloaded, true);
    }

    private void showDownloaded(boolean downloaded, boolean allowAnimate) {
        mDownloaded = downloaded;
        mReadPubButton.setChecked(mDownloaded, allowAnimate);

        ImageView iconView = (ImageView) mReadPubButton.findViewById(R.id.read_pub_icon);
        setOrAnimatePlusCheckIcon(iconView, downloaded, allowAnimate);
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
            mReadPubButton.setTranslationY(newTop + mHeaderHeightPixels
                    - mReadPubButtonHeightPixels / 2);
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

    private void setOrAnimatePlusCheckIcon(final ImageView imageView, boolean isCheck,
                                           boolean allowAnimate) {
        final int imageResId = isCheck
                ? R.drawable.read_pub_button_icon_checked
                : R.drawable.read_pub_button_icon_unchecked;

        if (imageView.getTag() != null) {
            if (imageView.getTag() instanceof Animator) {
                Animator anim = (Animator) imageView.getTag();
                anim.end();
                ViewHelper.setAlpha(imageView, 1f);
            }
        }

        if (allowAnimate && isCheck) {
            int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            Animator outAnimator = ObjectAnimator.ofFloat(imageView, "alpha", 0f);
            outAnimator.setDuration(duration / 2);
            outAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    imageView.setImageResource(imageResId);
                }
            });

            AnimatorSet inAnimator = new AnimatorSet();
            outAnimator.setDuration(duration);
            inAnimator.playTogether(
                    ObjectAnimator.ofFloat(imageView, "alpha", 1f),
                    ObjectAnimator.ofFloat(imageView, "scaleX", 0f, 1f),
                    ObjectAnimator.ofFloat(imageView, "scaleY", 0f, 1f)
            );

            AnimatorSet set = new AnimatorSet();
            set.playSequentially(outAnimator, inAnimator);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    imageView.setTag(null);
                }
            });
            imageView.setTag(set);
            set.start();
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageResource(imageResId);
                }
            });
        }
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
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mImages.add(img);

            if (getActivity() != null) {
                Glide.with(getActivity())
                        .load(url)
                        .placeholder(R.drawable.placeholder_img_art)
                        .centerCrop()
                        .crossFade()
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                // Trigger image transition
                                recomputePhotoAndScrollingMetrics();
                                return false;
                            }
                        })
                        .into(img);
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
