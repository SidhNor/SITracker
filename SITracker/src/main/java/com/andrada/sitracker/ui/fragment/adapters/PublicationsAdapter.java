/*
 * Copyright 2014 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui.fragment.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.contracts.IsNewItemTappedListener;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.PublicationMarkedAsReadEvent;
import com.andrada.sitracker.ui.HomeActivity;
import com.andrada.sitracker.ui.components.PublicationCategoryItemView;
import com.andrada.sitracker.ui.components.PublicationCategoryItemView_;
import com.andrada.sitracker.ui.components.PublicationItemView;
import com.andrada.sitracker.ui.components.PublicationItemView_;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.ImageLoader;

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

import de.greenrobot.event.EventBus;

@EBean
public class PublicationsAdapter extends BaseExpandableListAdapter implements
        IsNewItemTappedListener, AdapterView.OnItemLongClickListener {


    List<CategoryValue> mCategories = new ArrayList<CategoryValue>();
    List<List<Publication>> mChildren = new ArrayList<List<Publication>>();

    @OrmLiteDao(helper = SiDBHelper.class, model = Publication.class)
    PublicationDao publicationsDao;

    @RootContext
    Context context;

    @Pref
    SIPrefs_ prefs;

    private PublicationShareAttemptListener listener;

    private final HashMap<Long, Publication> mDownloadingPublications = new HashMap<Long, Publication>();

    @Nullable
    ImageLoader mLoader;

    @Nullable
    ListView listView = null;

    @Background
    public void reloadPublicationsForAuthorId(long id) {
        try {
            boolean shouldShowImages = prefs.displayPubImages().get();
            if (shouldShowImages) {
                mLoader = ((HomeActivity) context).getImageLoaderInstance();
            } else {
                mLoader = null;
            }
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
                } else if (publication.getNew()){
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
            e.printStackTrace();
        }
    }

    void updateAdapterDataSet(List<CategoryValue> newCategories, List<List<Publication>> newChildren) {
        mCategories = newCategories;
        mChildren = newChildren;
        postDataSetChanged();
    }

    @UiThread
    void postDataSetChanged() {
        notifyDataSetChanged();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        List<Publication> items = mChildren.get(groupPosition);
        return items.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        List<Publication> items = mChildren.get(groupPosition);
        return items.get(childPosition).getId();
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
        publicationItemView.bind(pub, mLoader);
        return publicationItemView;
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

    @Override
    public int getChildrenCount(int groupPosition) {
        return mChildren.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mCategories.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return mCategories.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
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
                mCategories.get(mCategories.indexOf(new CategoryValue(pub.getCategory()))).decrementNewCount();
                boolean authorNewChanged = publicationsDao.markPublicationRead(pub);
                EventBus.getDefault().post(new PublicationMarkedAsReadEvent(authorNewChanged));
            } catch (SQLException e) {
                AnalyticsHelper.getInstance().sendException("Publication Set update", e);
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
                AnalyticsHelper.getInstance().sendEvent(
                        Constants.GA_READ_CATEGORY,
                        Constants.GA_EVENT_AUTHOR_PUB_OPEN,
                        Constants.GA_EVENT_AUTHOR_PUB_OPEN);
            }
            // Return true as we are handling the event.
            return true;
        }

        return false;
    }

    public interface PublicationShareAttemptListener {
        void publicationShare(Publication pub, boolean forceDownload);
    }

    class CategoryValue {
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

        public boolean equals(String value) {
            return categoryName.equals(value);
        }

        @Override
        public int hashCode() {
            return categoryName.hashCode();
        }
    }
}
