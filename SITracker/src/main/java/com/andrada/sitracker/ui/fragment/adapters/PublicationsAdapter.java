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

package com.andrada.sitracker.ui.fragment.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.analytics.PublicationOpenedEvent;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.PublicationMarkedAsReadEvent;
import com.andrada.sitracker.ui.components.PublicationCategoryItemView;
import com.andrada.sitracker.ui.components.PublicationCategoryItemView_;
import com.andrada.sitracker.ui.components.PublicationItemView;
import com.andrada.sitracker.ui.components.PublicationItemView_;
import com.andrada.sitracker.analytics.AnalyticsManager;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

import static com.andrada.sitracker.util.LogUtils.LOGE;

@EBean
public class PublicationsAdapter extends BaseExpandableListAdapter implements
        IsNewItemTappedListener, AdapterView.OnItemLongClickListener {


    private final Map<Long, Publication> mDownloadingPublications = new HashMap<Long, Publication>();
    List<CategoryValue> mCategories = new ArrayList<CategoryValue>();
    List<List<Publication>> mChildren = new ArrayList<List<Publication>>();
    @OrmLiteDao(helper = SiDBHelper.class)
    PublicationDao publicationsDao;
    @RootContext
    Context context;
    @Pref
    SIPrefs_ prefs;
    @Nullable
    ListView listView = null;
    boolean shouldShowImages;
    private PublicationShareAttemptListener listener;
    private boolean showcaseViewShown = false;

    @Background
    public void reloadPublicationsForAuthorId(long id) {
        try {
            shouldShowImages = prefs.displayPubImages().get();
            List<Publication> pubs = publicationsDao.getSortedPublicationsForAuthorId(id);
            List<CategoryValue> newCategories = new ArrayList<CategoryValue>();
            List<List<Publication>> newChildren = new ArrayList<List<Publication>>();

            for (Publication publication : pubs) {
                CategoryValue possibleVal = new CategoryValue(publication.getCategory());
                if (!newCategories.contains(possibleVal)) {
                    if (publication.getNew()) {
                        possibleVal.incrementNewCount();
                    }
                    newCategories.add(possibleVal);
                } else if (publication.getNew()) {
                    newCategories.get(newCategories.indexOf(possibleVal)).incrementNewCount();
                }
            }

            for (CategoryValue category : newCategories) {
                List<Publication> categoryList = new ArrayList<Publication>();
                for (Publication publication : pubs) {
                    if (category.equals(publication.getCategory())) {
                        categoryList.add(publication);
                    }
                }
                newChildren.add(categoryList);
            }
            updateAdapterDataSet(newCategories, newChildren);
        } catch (SQLException e) {
            LOGE("SiTracker", "Exception while reloading pubs", e);
        }
    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    void updateAdapterDataSet(List<CategoryValue> newCategories, List<List<Publication>> newChildren) {
        mCategories = newCategories;
        mChildren = newChildren;
        postDataSetChanged();
    }

    @UiThread(delay = 300)
    void createAndShowShowcaseView(View view) {
        if (context instanceof Activity) {
            new ShowcaseView.Builder((Activity) context)
                    .setTarget(new ViewTarget(view))
                    .setContentTitle(context.getString(R.string.showcase_pub_quick_title))
                    .setContentText(context.getString(R.string.showcase_pub_quick_detail))
                    .setStyle(R.style.ShowcaseView_Base)
                    .singleShot(Constants.SHOWCASE_PUBLICATION_QUICK_ACCESS_SHOT_ID)
                    .build();
        }
    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    void postDataSetChanged() {
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return mCategories.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mChildren.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mCategories.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        List<Publication> items = mChildren.get(groupPosition);
        return items.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        List<Publication> items = mChildren.get(groupPosition);
        return items.get(childPosition).getId();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @NotNull
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             @Nullable View convertView, ViewGroup parent) {
        if (listView == null) {
            listView = (ListView) parent;
        }
        PublicationCategoryItemView publicationCategoryView;
        //For some weird reason, convertView is PublicationItemView instead of PublicationCategoryItemView_
        if (!(convertView instanceof PublicationCategoryItemView)) {
            publicationCategoryView = PublicationCategoryItemView_.build(context);
        } else {
            publicationCategoryView = (PublicationCategoryItemView) convertView;
        }
        publicationCategoryView.bind(mCategories.get(groupPosition).categoryName,
                mChildren.get(groupPosition).size(), mCategories.get(groupPosition).getNewCount());
        return publicationCategoryView;
    }

    @NotNull
    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, @Nullable View convertView, ViewGroup parent) {

        Publication pub = (Publication) getChild(groupPosition, childPosition);

        PublicationItemView publicationItemView;
        if (convertView == null) {
            publicationItemView = PublicationItemView_.build(context);
            publicationItemView.setListener(this);
        } else {
            publicationItemView = (PublicationItemView) convertView;
        }
        publicationItemView.bind(pub, shouldShowImages);

        if (!showcaseViewShown) {
            showcaseViewShown = true;
            createAndShowShowcaseView(publicationItemView);
        }

        return publicationItemView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public void onIsNewItemTapped(@NotNull View checkBox) {
        if (listView != null) {
            Publication pub = (Publication) checkBox.getTag();
            if (pub != null) {
                updateStatusOfPublication(pub);
            }
        }
    }

    public void stopProgressOnPublication(long id, boolean success) {
        Publication loadingPub = mDownloadingPublications.get(id);
        if (loadingPub != null) {
            loadingPub.setLoading(false);
            mDownloadingPublications.remove(id);
            if (success) {
                updateStatusOfPublication(loadingPub);
            }
        }
        notifyDataSetChanged();
    }

    public void setShareListener(PublicationShareAttemptListener listener) {
        this.listener = listener;
    }

    @Background
    protected void updateStatusOfPublication(@NotNull Publication pub) {
        if (pub.getNew()) {
            try {
                int index = mCategories.indexOf(new CategoryValue(pub.getCategory()));
                if (index >= 0 && index < mCategories.size()) {
                    mCategories.get(index).decrementNewCount();
                }
                boolean authorNewChanged = publicationsDao.markPublicationRead(pub);
                EventBus.getDefault().post(new PublicationMarkedAsReadEvent(authorNewChanged));
            } catch (SQLException e) {
                AnalyticsManager.getInstance().sendException("Publication Set update", e);
            }
            postDataSetChanged();
        }
    }

    @Override
    public boolean onItemLongClick(@NotNull AdapterView<?> parent, View view, int position, long id) {
        if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            long packedPosition = ((ExpandableListView) parent).getExpandableListPosition(position);
            int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
            int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
            List<Publication> items = mChildren.get(groupPosition);
            Publication pub = items.get(childPosition);

            if (pub.getLoading()) {
                //Ignore if it is loading now
                return true;
            }
            if (listener != null) {
                //Mark item as loading
                mDownloadingPublications.put(pub.getId(), pub);
                pub.setLoading(true);
                notifyDataSetChanged();

                //Attempt to open or download publication
                listener.publicationShare(pub, pub.getNew());

                AnalyticsManager.getInstance().logEvent(new PublicationOpenedEvent(pub.getName(), false));
            }
            // Return true as we are handling the event.
            return true;
        }

        return false;
    }

    public interface PublicationShareAttemptListener {
        void publicationShare(Publication pub, boolean forceDownload);
    }

    public class CategoryValue {
        public final String categoryName;
        private int newCount;

        private CategoryValue(@NotNull String categoryName) {
            this.categoryName = categoryName;
            this.newCount = 0;
        }

        public void incrementNewCount() {
            ++newCount;
        }

        public void decrementNewCount() {
            --newCount;
        }

        public int getNewCount() {
            return newCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CategoryValue that = (CategoryValue) o;

            return categoryName.equals(that.categoryName);

        }

        @Override
        public int hashCode() {
            return categoryName.hashCode();
        }

        public boolean equals(String value) {
            return categoryName.equals(value);
        }
    }
}
