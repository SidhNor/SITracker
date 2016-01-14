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

package com.andrada.sitracker.ui.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.andrada.sitracker.BuildConfig;
import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.PublicationMarkedAsReadEvent;
import com.andrada.sitracker.events.RatingResultEvent;
import com.andrada.sitracker.exceptions.SharePublicationException;
import com.andrada.sitracker.reader.SamlibPublicationPageReader;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.widget.MessageCardView;
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
import com.github.amlcurran.showcaseview.targets.ViewTarget;
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

import static com.andrada.sitracker.util.LogUtils.LOGI;

@EFragment(R.layout.fragment_pub_details)
@OptionsMenu(R.menu.publication_info_menu)
public class PublicationInfoFragment extends BaseFragment {

    private static final float PHOTO_ASPECT_RATIO = 1.7777777f;

    @OrmLiteDao(helper = SiDBHelper.class)
    PublicationDao publicationsDao;

    Publication currentRecord;
    @Pref
    SIPrefs_ prefs;

    @ViewById(R.id.pub_title)
    TextView mTitle;

    @ViewById(R.id.pub_subtitle)
    TextView mSubtitle;

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

    @ViewById(R.id.details_container)
    View mDetailsContainer;
    @ViewById(R.id.pub_photo_container)
    View mPhotoViewContainer;
    @ViewById(R.id.pager)
    ViewPager pager;
    @ViewById(R.id.pagerIndicators)
    CirclePageIndicator pagerIndicators;

    @ViewById(R.id.read_pub_button)
    FloatingActionButton mReadPubButton;

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
    private long mPublicationId;

    private boolean mHasPhoto;
    private boolean rateShowcaseShown;
    private boolean mRatingVisible;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGI("SITracker", "PublicationInfoFragment - onCreate");
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        Uri mPublicationUri = intent.getData();

        if (mPublicationUri == null) {
            return;
        }
        mPublicationId = AppUriContract.getPublicationId(mPublicationUri);
        mHandler = new Handler();
        rateShowcaseShown = prefs.ratingShowcaseShotDone().get();

        AnalyticsHelper.getInstance().sendView(Constants.GA_SCREEN_PUBLICATION_INFO);
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

    @Override
    public void onDestroy() {
        OpenHelperManager.releaseHelper();
        BackgroundExecutor.cancelAll("publicationDownload", true);
        super.onDestroy();
        LOGI("SITracker", "PublicationInfoFragment - onDestroy");
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
        pager.setAdapter(new PublicationImagesAdapter(getActivity()));
        pagerIndicators.setViewPager(pager);
        pagerIndicators.setSnap(true);
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
                    SpannableStringBuilder snackbarText = new SpannableStringBuilder();
                    snackbarText.append(msg);
                    snackbarText.setSpan(new ForegroundColorSpan(Color.RED), 0, snackbarText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    Snackbar.make(getView(), snackbarText, Snackbar.LENGTH_SHORT).show();
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
            recomputePhotoMetrics();
        }

        //Check if file is new version of pub is loaded.
        if (mIsDownloading) {
            showPublicationState(PublicationState.DOWNLOADING, false);
        } else {
            boolean isRefreshable = false;
            if (getActivity() != null) {
                isRefreshable = ShareHelper.shouldRefreshPublication(getActivity(), currentRecord, prefs.downloadFolder().get());
            }
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
                ActivityCompat.startPostponedEnterTransition(getActivity());
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
            mRatingVisible = true;

        } else {
            mRatingContainer.setVisibility(View.GONE);
            mRatingVisible = false;
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
            AnalyticsHelper.getInstance().sendException("Could not load publication image mRecyclerView", e);
            addImagesToList(new ArrayList<Pair<String, String>>());
        }
    }

    @Background
    void markCurrentPublicationRead() {
        try {
            publicationsDao.markPublicationRead(currentRecord);
            EventBus.getDefault().post(new PublicationMarkedAsReadEvent(true));
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
        recomputePhotoMetrics();
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

    //TODO decide when to show this
    @UiThread(delay = 500)
    void attemptToShowShowcaseForRatings() {
        if (!rateShowcaseShown && mRatingVisible) {
            rateShowcaseShown = true;
            prefs.ratingShowcaseShotDone().put(true);
            ShowcaseView.Builder bldr = new ShowcaseView.Builder(getActivity())
                    .setTarget(new ViewTarget(mRatingBar))
                    .setContentTitle(getString(R.string.showcase_pub_detail_ratings_title))
                    .setContentText(getString(R.string.showcase_pub_detail_ratings_detail))
                    .setStyle(R.style.ShowcaseView_Base_Overlayed);
            if (!BuildConfig.DEBUG) {
                bldr.singleShot(Constants.SHOWCASE_PUBLICATION_DETAIL_RATING_SHOT_ID);
            }
            bldr.build();
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
        SpannableStringBuilder snackbarText = new SpannableStringBuilder();
        snackbarText.append(msg);
        if (!result.ratingSubmissionResult) {
            snackbarText.setSpan(new ForegroundColorSpan(0xFFFF0000), 0, snackbarText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        Snackbar.make(getView(), snackbarText, Snackbar.LENGTH_SHORT).show();
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

    private void showPublicationState(PublicationState state, boolean allowAnimate) {
        if (!isDetached()) {
            mDownloaded = state.equals(PublicationState.READY_FOR_READING);
            if (!mIsDownloading && mDownloaded) {
                mReadPubButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            }
            setOrAnimateReadPubIcon(mReadPubButton, state, allowAnimate);
        }
    }

    private void recomputePhotoMetrics() {

        int mPhotoHeightPixels = 4;
        if (mHasPhoto) {
            mPhotoHeightPixels = (int) (mPhotoViewContainer.getWidth() / PHOTO_ASPECT_RATIO);
            mPhotoHeightPixels = Math.min(mPhotoHeightPixels, mRootView.getHeight() * 2 / 3);
        }

        ViewGroup.LayoutParams lp;
        lp = mPhotoViewContainer.getLayoutParams();
        if (lp.height != mPhotoHeightPixels) {
            lp.height = mPhotoHeightPixels;
        }
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

        if (allowAnimate && currentState.equals(PublicationState.DOWNLOADING)) {
            int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            Animator outAnimator = ObjectAnimator.ofFloat(imageView, "alpha", 0f);
            outAnimator.setDuration(duration);
            outAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Drawable frameAnimation = ContextCompat.getDrawable(getActivity(), imageResId);
                    imageView.setImageDrawable(frameAnimation);
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
                    Drawable frameAnimation = ContextCompat.getDrawable(getActivity(), imageResId);
                    imageView.setImageDrawable(frameAnimation);

                    if (frameAnimation instanceof AnimationDrawable) {
                        ((AnimationDrawable) frameAnimation).start();
                    }
                }
            });
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
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
            mImages = new ArrayList<>();
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
                                        recomputePhotoMetrics();
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
