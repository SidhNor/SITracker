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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.format.DateUtils;
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
import android.widget.RatingBar;
import android.widget.TextView;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.RatingResultEvent;
import com.andrada.sitracker.exceptions.SharePublicationException;
import com.andrada.sitracker.reader.SamlibPublicationPageReader;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.widget.CheckableFrameLayout;
import com.andrada.sitracker.ui.widget.MessageCardView;
import com.andrada.sitracker.ui.widget.ObservableScrollView;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.SamlibPageHelper;
import com.andrada.sitracker.util.ShareHelper;
import com.andrada.sitracker.util.UIUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.android.gms.plus.PlusOneButton;
import com.j256.ormlite.android.apptools.OpenHelperManager;
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
import org.androidannotations.api.BackgroundExecutor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

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
    @ViewById(R.id.publication_rating_block)
    ViewGroup mRatingContainer;
    @ViewById(R.id.publication_rating_text)
    TextView mPubRating;
    @ViewById(R.id.publication_rating_count)
    TextView mPubRatingCount;
    @ViewById(R.id.publication_rating)
    RatingBar mRatingBar;
    @ViewById(R.id.voted_on_field)
    TextView mVotedOnField;
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
    @ViewById(R.id.plus_one_button)
    PlusOneButton mPlusOneButton;
    @OptionsMenuItem(R.id.action_mark_read)
    MenuItem mMarkAsReadAction;
    @OptionsMenuItem(R.id.action_force_download)
    MenuItem mForceDownloadAction;
    @InstanceState
    boolean mIsDownloading = false;

    private boolean mDownloaded;
    private ViewGroup mRootView;
    private Handler mHandler = new Handler();
    private Uri mPublicationUri;
    private long mPublicationId;
    private boolean mHasPhoto;
    private boolean mGapFillShown;
    private float newTop;
    private int mHeaderTopClearance;
    private int mPhotoHeightPixels;
    private int mHeaderHeightPixels;
    private int mReadPubButtonHeightPixels;
    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            mReadPubButtonHeightPixels = mReadPubButton.getHeight();
            recomputePhotoAndScrollingMetrics();
        }
    };

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
        EventBus.getDefault().register(this);
        loadData();
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mScrollView == null) {
            return;
        }
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        if (vto.isAlive()) {
            //noinspection deprecation
            if (UIUtils.hasJellyBean()) {
                vto.removeOnGlobalLayoutListener(mGlobalLayoutListener);
            } else {
                vto.removeGlobalOnLayoutListener(mGlobalLayoutListener);
            }

        }
    }

    @Override
    public void onDestroy() {
        OpenHelperManager.releaseHelper();
        BackgroundExecutor.cancelAll("publicationDownload", true);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mIsDownloading) {
            mForceDownloadAction.setActionView(R.layout.ab_download_progress);
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

    @Background(id = "publicationDownload")
    void downloadPublication(boolean forceDownload, boolean startActivity) {
        int errorMessage = -1;
        try {
            final Intent intent = ShareHelper.fetchPublication(getActivity(), currentRecord, prefs.downloadFolder().get(), forceDownload);
            markCurrentPublicationRead();
            if (startActivity && getActivity() != null) {
                mIsDownloading = false;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showPublicationState(PublicationState.READY_FOR_READING, true);
                    }
                });
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() != null) {
                            getActivity().startActivity(intent);
                        }
                    }
                }, 1000);
            }
        } catch (SharePublicationException e) {
            if (getActivity() == null) {
                //Fragment is detached, we won't be abel to show anything
                return;
            }
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
                    showPublicationState(PublicationState.WAITING_REFRESH, false);
                    showCustomPositionedCrouton(msg, false);
                }
            });
        } finally {
            mIsDownloading = false;
            if (getActivity() != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mForceDownloadAction.setActionView(null);
                    }
                });
            }
        }
    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    void bindData() {
        String mTitleString = currentRecord.getName();
        String subtitle = currentRecord.getCategory();

        mTitle.setText(mTitleString);
        mSubtitle.setText(subtitle);

        mReadPubButton.setVisibility(View.VISIBLE);
        mPhotoViewContainer.setBackgroundColor(UIUtils.scaleColor(0xe8552c, 0.65f, false));

        updatePlusOneButton();
        if (currentRecord.getUpdatesIgnored()) {
            showEnableUpdatesBackCard();
        } else {
            final MessageCardView messageCardView = (MessageCardView) mRootView.findViewById(
                    R.id.message_card_view);
            if (messageCardView != null) {
                messageCardView.setVisibility(View.GONE);
            }
        }
        updateRating();

        String imagesUrl = currentRecord.getImagePageUrl();
        if (!TextUtils.isEmpty(imagesUrl) && prefs.displayPubImages().get()) {
            //Do a network request to detect number of images
            loadImageList(imagesUrl);
            //Add images
            //Add image view to pager.
        } else {
            mHasPhoto = false;
            recomputePhotoAndScrollingMetrics();
        }

        //Check if file is new version of pub is loaded.
        if (mIsDownloading) {
            showPublicationState(PublicationState.DOWNLOADING, false);
        } else {
            boolean isRefreshable = ShareHelper.shouldRefreshPublication(getActivity(), currentRecord, prefs.downloadFolder().get());
            if (isRefreshable) {
                showPublicationState(PublicationState.WAITING_REFRESH, false);
            } else {
                showPublicationState(PublicationState.READY_FOR_READING, false);
            }
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

    private void updateRating() {
        String ratingString = currentRecord.getRating();
        if (!TextUtils.isEmpty(ratingString) && ratingString.split("\\*").length == 2) {
            float rating = Float.valueOf(ratingString.split("\\*")[0]);
            int ratingCount = Integer.valueOf(ratingString.split("\\*")[1]);
            mRatingContainer.setVisibility(View.VISIBLE);
            mPubRating.setText(String.valueOf(rating));
            mPubRatingCount.setText(String.valueOf(ratingCount));

            //Handle personal rating
            if (!TextUtils.isEmpty(currentRecord.getVoteCookie()) && currentRecord.getVoteDate() != null) {
                mRatingBar.setRating(currentRecord.getMyVote());
                mVotedOnField.setVisibility(View.VISIBLE);
                String formattedVoteDate = DateUtils.getRelativeTimeSpanString(
                        currentRecord.getVoteDate().getTime(), new Date().getTime(),
                        DateUtils.MINUTE_IN_MILLIS).toString();
                mVotedOnField.setText(getString(R.string.publication_rating_vote_date, formattedVoteDate));
            } else {
                mRatingBar.setRating(rating);
                mVotedOnField.setVisibility(View.GONE);
            }

        } else {
            mRatingContainer.setVisibility(View.GONE);
        }
    }

    private void updatePlusOneButton() {
        if (mPlusOneButton == null) {
            return;
        }
        if (currentRecord == null) {
            return;
        }

        if (!TextUtils.isEmpty(currentRecord.getUrl())) {
            mPlusOneButton.initialize(currentRecord.getUrl(), 0);
            mPlusOneButton.setVisibility(View.VISIBLE);
        } else {
            mPlusOneButton.setVisibility(View.GONE);
        }
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
            AnalyticsHelper.getInstance().sendException("Could not load publication image list", e);
            addImagesToList(new ArrayList<Pair<String, String>>());
        }
    }

    @Background
    void markCurrentPublicationRead() {
        try {
            publicationsDao.markPublicationRead(currentRecord);
        } catch (SQLException e) {
            AnalyticsHelper.getInstance().sendException(e);
        }
    }

    @Background
    void markPublicationAsIgnored(boolean ignored) {
        if (currentRecord != null) {
            try {
                currentRecord.setUpdatesIgnored(ignored);
                publicationsDao.update(currentRecord);
                bindData();
            } catch (SQLException e) {
                AnalyticsHelper.getInstance().sendException(e);
            }
        }
    }

    @Background
    void saveVoteResult(String voteCookie, int rating) {
        try {
            currentRecord.setMyVote(rating);
            currentRecord.setVoteCookie(voteCookie);
            currentRecord.setVoteDate(new Date());
            publicationsDao.update(currentRecord);
            bindData();
        } catch (SQLException e) {
            AnalyticsHelper.getInstance().sendException(e);
        }
    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    void addImagesToList(List<Pair<String, String>> results) {
        if (results.size() == 0) {
            mHasPhoto = false;
        } else {
            mHasPhoto = true;
            if (results.size() == 1) {
                pagerIndicators.setVisibility(View.GONE);
            } else if (results.size() > 15) {
                results = results.subList(0, 15);
            }
            PublicationImagesAdapter adapter = (PublicationImagesAdapter) pager.getAdapter();
            adapter.removeAllItems();
            for (Pair<String, String> res : results) {
                adapter.addImage(res.first);
            }
            attemptToShowShowcaseForImageSettings();
        }
        recomputePhotoAndScrollingMetrics();
    }

    @UiThread(delay = 500)
    void attemptToShowShowcaseForImageSettings() {
        if (getActivity() != null) {
            Rect pointOnImage = new Rect();
            mPhotoViewContainer.getHitRect(pointOnImage);
            int x = (int) (pointOnImage.left + pointOnImage.right / 1.3);
            int y = pointOnImage.top + pointOnImage.bottom / 2;

            new ShowcaseView.Builder(getActivity())
                    .setTarget(new PointTarget(x, y))
                    .setContentTitle(getString(R.string.showcase_pub_detail_image_title))
                    .setContentText(getString(R.string.showcase_pub_detail_image_detail))
                    .setStyle(R.style.ShowcaseView_Base_Overlayed)
                    .singleShot(Constants.SHOWCASE_PUBLICATION_DETAIL_IMAGES_SHOT_ID)
                    .build();
        }
    }

    @OptionsItem(R.id.action_mark_read)
    void menuMarkAsReadSelected() {
        markCurrentPublicationRead();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mMarkAsReadAction.setVisible(false);
            }
        });
    }

    @OptionsItem(R.id.action_open_pub_in_browser)
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
            mForceDownloadAction.setActionView(R.layout.ab_download_progress);
            //Start downloading in a background thread
            //Do force, no activity start
            downloadPublication(true, false);
            AnalyticsHelper.getInstance().sendEvent(
                    Constants.GA_ADMIN_CATEGORY,
                    Constants.GA_EVENT_PUB_MANUAL_REFRESH,
                    Constants.GA_EVENT_AUTHOR_PUB_OPEN);
        }
    }

    @OptionsItem(R.id.action_ignore_updates)
    void ignoreUpdatesSelected() {
        AnalyticsHelper.getInstance().sendEvent(Constants.GA_ADMIN_CATEGORY,
                Constants.GA_EVENT_PUB_IGNORED,
                Constants.GA_EVENT_PUB_IGNORED);
        markPublicationAsIgnored(true);
    }

    @Click(R.id.read_pub_button)
    void downloadAndReadButtonClicked() {
        if (mIsDownloading) {
            return;
        }
        AnalyticsHelper.getInstance().sendEvent(
                Constants.GA_READ_CATEGORY,
                Constants.GA_EVENT_FAB_CLICK,
                Constants.GA_EVENT_AUTHOR_PUB_OPEN);
        if (!mDownloaded) {
            //Start download here
            mIsDownloading = true;
            showPublicationState(PublicationState.DOWNLOADING, true);
            downloadPublication(false, true);

        } else {
            //Open the pub right away
            downloadPublication(false, true);
        }
    }

    @Click(R.id.publication_rating_block)
    void voteForPubClicked() {
        if (currentRecord != null && getActivity() != null) {
            AnalyticsHelper.getInstance().sendView(Constants.GA_SCREEN_RATING_DIALOG);
            FragmentManager fm = this.getActivity().getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment prev = fm.findFragmentByTag(RatePublicationDialog.FRAGMENT_TAG);
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            RatePublicationDialog dg;
            if (currentRecord.getVoteDate() != null && !TextUtils.isEmpty(currentRecord.getVoteCookie())) {
                dg = RatePublicationDialog_.builder()
                        .publicationUrl(currentRecord.getUrl())
                        .currentRating(currentRecord.getMyVote())
                        .votingCookie(currentRecord.getVoteCookie())
                        .build();
            } else {
                dg = RatePublicationDialog_.builder()
                        .publicationUrl(currentRecord.getUrl())
                        .build();
            }
            dg.show(ft, AboutDialog.FRAGMENT_TAG);

        }
    }

    public void onEventMainThread(RatingResultEvent result) {
        String msg;
        if (result.ratingSubmissionResult) {
            msg = getString(R.string.publication_rating_submit_success);
            saveVoteResult(result.voteCookie, result.ratingValue);
        } else {
            msg = getString(R.string.publication_rating_submit_error);
        }
        showCustomPositionedCrouton(msg, result.ratingSubmissionResult);

    }

    private void showEnableUpdatesBackCard() {
        final MessageCardView messageCardView = (MessageCardView) mRootView.findViewById(
                R.id.message_card_view);
        messageCardView.show();
        messageCardView.setListener(new MessageCardView.OnMessageCardButtonClicked() {
            @Override
            public void onMessageCardButtonClicked(String tag) {
                if ("ENABLE_UPDATES_BACK".equals(tag)) {
                    AnalyticsHelper.getInstance().sendEvent(
                            Constants.GA_ADMIN_CATEGORY,
                            Constants.GA_EVENT_ENABLE_UPDATES_BACK,
                            Constants.GA_EVENT_ENABLE_UPDATES_BACK);
                    markPublicationAsIgnored(false);
                } else {
                    messageCardView.dismiss(true);
                }
            }
        });
    }

    private void showCustomPositionedCrouton(String message, boolean success) {
        if (getActivity() == null) {
            return;
        }
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.crouton_custom_pos_textview, null);
        if (success) {
            view.findViewById(android.R.id.background).setBackgroundColor(Style.holoGreenLight);
        } else {
            view.findViewById(android.R.id.background).setBackgroundColor(Style.holoRedLight);
        }
        int topPadding = UIUtils.calculateActionBarSize(getActivity());
        if (!mHasPhoto) {
            topPadding = (int) (newTop + mHeaderHeightPixels);
        }
        view.setPadding(view.getPaddingLeft(), topPadding,
                view.getPaddingRight(), view.getPaddingBottom());
        TextView tv = (TextView) view.findViewById(android.R.id.text1);
        tv.setText(message);
        Crouton cr = Crouton.make(getActivity(), view);
        cr.setConfiguration(new Configuration.Builder()
                .setDuration(Configuration.DURATION_LONG).build());
        cr.show();
    }

    private void showPublicationState(PublicationState state, boolean allowAnimate) {
        if (!isDetached()) {
            mDownloaded = state.equals(PublicationState.READY_FOR_READING);
            if (!mIsDownloading) {
                mReadPubButton.setChecked(mDownloaded, allowAnimate);
            }
            ImageView iconView = (ImageView) mReadPubButton.findViewById(R.id.read_pub_icon);
            setOrAnimateReadPubIcon(iconView, state, allowAnimate);
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

        newTop = Math.max(mPhotoHeightPixels, scrollY + mHeaderTopClearance);

        mHeaderBox.setTranslationY(newTop);
        mReadPubButton.setTranslationY(newTop + mHeaderHeightPixels
                - mReadPubButtonHeightPixels / 2);
        mHeaderBackgroundBox.setPivotY(mHeaderHeightPixels);
        int gapFillDistance = (int) (mHeaderTopClearance * GAP_FILL_DISTANCE_MULTIPLIER);
        boolean showGapFill = !mHasPhoto || (scrollY > (mPhotoHeightPixels - gapFillDistance));
        float desiredHeaderScaleY = showGapFill ?
                ((mHeaderHeightPixels + gapFillDistance + 1) * 1f / mHeaderHeightPixels)
                : 1f;

        if (!mHasPhoto) {
            mHeaderBackgroundBox.setScaleY(desiredHeaderScaleY);
        } else if (mGapFillShown != showGapFill) {
            mHeaderBackgroundBox.animate()
                    .scaleY(desiredHeaderScaleY)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .setDuration(250)
                    .start();

        }
        mGapFillShown = showGapFill;

        mHeaderShadow.setVisibility(View.VISIBLE);

        if (mHeaderTopClearance != 0) {
            // Fill the gap between status bar and header bar with color
            float gapFillProgress = Math.min(Math.max(UIUtils.getProgress(scrollY,
                    mPhotoHeightPixels - mHeaderTopClearance * 2,
                    mPhotoHeightPixels - mHeaderTopClearance), 0), 1);
            mHeaderShadow.setAlpha(gapFillProgress);

        }

        // Move background photo (parallax effect)
        mPhotoViewContainer.setTranslationY(scrollY * 0.3f);

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

        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)
                mDetailsContainer.getLayoutParams();
        if (mlp.topMargin != mHeaderHeightPixels + mPhotoHeightPixels) {
            mlp.topMargin = mHeaderHeightPixels + mPhotoHeightPixels;
            mDetailsContainer.setLayoutParams(mlp);
        }
        onScrollChanged(0, 0); // trigger scroll handling
    }

    private void setOrAnimateReadPubIcon(final ImageView imageView, PublicationState currentState,
                                         boolean allowAnimate) {
        final int imageResId = currentState.equals(PublicationState.READY_FOR_READING)
                ? R.drawable.read_pub_button_icon_checked
                : currentState.equals(PublicationState.DOWNLOADING) ? R.drawable.download_pub_icon_fab_up
                : R.drawable.read_pub_button_icon_unchecked;

        if (imageView.getTag() != null) {
            if (imageView.getTag() instanceof Animator) {
                Animator anim = (Animator) imageView.getTag();
                anim.end();
                imageView.setAlpha(1f);
            }
        }
        /*
        if (imageView.getBackground() instanceof AnimationDrawable) {
            AnimationDrawable frameAnimation = (AnimationDrawable) imageView.getBackground();
            frameAnimation.stop();
            imageView.setBackgroundResource(0);
        }*/

        if (allowAnimate && currentState.equals(PublicationState.DOWNLOADING)) {
            int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            Animator outAnimator = ObjectAnimator.ofFloat(imageView, "alpha", 0f);
            outAnimator.setDuration(duration);
            outAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    imageView.setImageDrawable(null);
                    imageView.setBackgroundResource(imageResId);
                    Drawable frameAnimation = imageView.getBackground();
                    if (frameAnimation instanceof AnimationDrawable) {
                        ((AnimationDrawable) frameAnimation).start();
                    }
                }
            });

            ObjectAnimator inAnimator = ObjectAnimator.ofFloat(imageView, "alpha", 1f);
            inAnimator.setDuration(duration * 2);
            final AnimatorSet set = new AnimatorSet();
            set.playSequentially(outAnimator, inAnimator);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    imageView.setTag(null);
                }
            });
            imageView.setTag(set);
            set.start();

        } else if (allowAnimate && currentState.equals(PublicationState.READY_FOR_READING)) {
            int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            Animator outAnimator = ObjectAnimator.ofFloat(imageView, "alpha", 0f);
            outAnimator.setDuration(duration);
            outAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    imageView.setBackgroundResource(0);
                    imageView.setImageResource(imageResId);
                }
            });
            ObjectAnimator inAnimator = ObjectAnimator.ofFloat(imageView, "alpha", 1f);
            inAnimator.setDuration(duration * 2);
            final AnimatorSet set = new AnimatorSet();
            set.playSequentially(outAnimator, inAnimator);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    imageView.setTag(null);
                }
            });
            imageView.setTag(set);
            set.start();
        } else if (!allowAnimate && currentState.equals(PublicationState.DOWNLOADING)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageDrawable(null);
                    imageView.setBackgroundResource(imageResId);
                    AnimationDrawable frameAnimation = (AnimationDrawable) imageView.getBackground();
                    frameAnimation.start();
                }
            });
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    imageView.setBackgroundResource(0);
                    imageView.setImageResource(imageResId);
                }
            });
        }
    }

    enum PublicationState {
        WAITING_REFRESH,
        DOWNLOADING,
        READY_FOR_READING
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
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        recomputePhotoAndScrollingMetrics();
                                    }
                                });
                                return false;
                            }
                        }).into(img);
            }
            notifyDataSetChanged();
        }

        public void removeAllItems() {
            mImages.clear();
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
