package com.andrada.sitracker.ui.fragment;

import android.app.Fragment;
import android.support.v7.widget.RecyclerView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.util.NavDrawerManager;

import org.androidannotations.annotations.EFragment;

@EFragment(R.layout.fragment_explore_authors)
public class ExploreAuthorsFragment extends Fragment
        implements NavDrawerManager.NavDrawerItemAware {

    @Override
    public int getSelfNavDrawerItem() {
        return NavDrawerManager.NAVDRAWER_ITEM_EXPLORE;
    }

    @Override
    public void setContentTopClearance(int top) {

    }

    @Override
    public boolean canCollectionViewScrollUp() {
        return false;
    }

    @Override
    public RecyclerView getRecyclerView() {
        return null;
    }
}
