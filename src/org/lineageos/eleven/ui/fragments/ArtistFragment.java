/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.lineageos.eleven.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import org.lineageos.eleven.MusicStateListener;
import org.lineageos.eleven.R;
import org.lineageos.eleven.adapters.ArtistAdapter;
import org.lineageos.eleven.adapters.PagerAdapter;
import org.lineageos.eleven.loaders.ArtistLoader;
import org.lineageos.eleven.model.Artist;
import org.lineageos.eleven.recycler.RecycleHolder;
import org.lineageos.eleven.sectionadapter.SectionAdapter;
import org.lineageos.eleven.sectionadapter.SectionCreator;
import org.lineageos.eleven.sectionadapter.SectionListContainer;
import org.lineageos.eleven.ui.activities.BaseActivity;
import org.lineageos.eleven.ui.fragments.phone.MusicBrowserFragment;
import org.lineageos.eleven.utils.ArtistPopupMenuHelper;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.NavUtils;
import org.lineageos.eleven.utils.PopupMenuHelper;
import org.lineageos.eleven.utils.SectionCreatorUtils;
import org.lineageos.eleven.utils.SectionCreatorUtils.IItemCompare;
import org.lineageos.eleven.widgets.IPopupMenuCallback;
import org.lineageos.eleven.widgets.LoadingEmptyContainer;

/**
 * This class is used to display all of the artists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistFragment extends MusicBrowserFragment implements
        LoaderCallbacks<SectionListContainer<Artist>>,
        OnScrollListener, OnItemClickListener, MusicStateListener {

    /**
     * Fragment UI
     */
    private ViewGroup mRootView;

    /**
     * The adapter for the grid
     */
    private SectionAdapter<Artist, ArtistAdapter> mAdapter;

    /**
     * The list view
     */
    private ListView mListView;

    /**
     * Pop up menu helper
     */
    private PopupMenuHelper mPopupMenuHelper;

    /**
     * Loading container and no results container
     */
    private LoadingEmptyContainer mLoadingEmptyContainer;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public ArtistFragment() {
    }

    @Override
    public int getLoaderId() {
        return PagerAdapter.MusicFragments.ARTIST.ordinal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPopupMenuHelper = new ArtistPopupMenuHelper(getActivity(), getFragmentManager()) {
            @Override
            public Artist getArtist(int position) {
                return mAdapter.getTItem(position);
            }
        };

        // Create the adapter
        final int layout = R.layout.list_item_normal;
        ArtistAdapter adapter = new ArtistAdapter(getActivity(), layout);
        mAdapter = new SectionAdapter<>(getActivity(), adapter);
        mAdapter.setPopupMenuClickedListener(new IPopupMenuCallback.IListener() {
            @Override
            public void onPopupMenuClicked(View v, int position) {
                mPopupMenuHelper.showPopupMenu(v, position);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        // The View for the fragment's UI
        mRootView = (ViewGroup)inflater.inflate(R.layout.list_base, null);
        initListView();

        // Register the music status listener
        ((BaseActivity)getActivity()).setMusicStateListenerListener(this);

        return mRootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ((BaseActivity)getActivity()).removeMusicStateListenerListener(this);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        // Start the loader
        initLoader(null, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        mAdapter.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
        // Pause disk cache access to ensure smoother scrolling
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            mAdapter.getUnderlyingAdapter().setPauseDiskCache(true);
        } else {
            mAdapter.getUnderlyingAdapter().setPauseDiskCache(false);
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        Artist artist = mAdapter.getTItem(position);
        NavUtils.openArtistProfile(getActivity(), artist.mArtistName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Loader<SectionListContainer<Artist>> onCreateLoader(final int id, final Bundle args) {
        mLoadingEmptyContainer.showLoading();
        final Context context = getActivity();
        IItemCompare<Artist> comparator = SectionCreatorUtils.createArtistComparison(context);
        return new SectionCreator<>(getActivity(), new ArtistLoader(context), comparator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(final Loader<SectionListContainer<Artist>> loader,
                               final SectionListContainer<Artist> data) {
        if (data.mListResults.isEmpty()) {
            mAdapter.unload();
            mLoadingEmptyContainer.showNoResults();
            return;
        }

        mAdapter.setData(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(final Loader<SectionListContainer<Artist>> loader) {
        // Clear the data in the adapter
        mAdapter.unload();
    }

    /**
     * Scrolls the list to the currently playing artist when the user touches
     * the header in the {@link TitlePageIndicator}.
     */
    public void scrollToCurrentArtist() {
        final int currentArtistPosition = getItemPositionByArtist();

        if (currentArtistPosition != 0) {
            mListView.setSelection(currentArtistPosition);
        }
    }

    /**
     * @return The position of an item in the list or grid based on the name of
     *         the currently playing artist.
     */
    private int getItemPositionByArtist() {
        final long artistId = MusicUtils.getCurrentArtistId();
        if (mAdapter == null) {
            return 0;
        }

        int position = mAdapter.getItemPosition(artistId);

        // if for some reason we don't find the item, just jump to the top
        if (position < 0) {
            return 0;
        }

        return position;
    }

    /**
     * Restarts the loader.
     */
    public void refresh() {
        // Wait a moment for the preference to change.
        SystemClock.sleep(10);
        restartLoader();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScroll(final AbsListView view, final int firstVisibleItem,
            final int visibleItemCount, final int totalItemCount) {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restartLoader() {
        // Update the list when the user deletes any items
        restartLoader(null, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMetaChanged() {
        // Nothing to do
    }

    @Override
    public void onPlaylistChanged() {
        // Nothing to do
    }

    /**
     * Sets up various helpers for both the list and grid
     *
     * @param list The list or grid
     */
    private void initAbsListView(final AbsListView list) {
        // Release any references to the recycled Views
        list.setRecyclerListener(new RecycleHolder());
        // Show the albums and songs from the selected artist
        list.setOnItemClickListener(this);
        // To help make scrolling smooth
        list.setOnScrollListener(this);
    }

    /**
     * Sets up the list view
     */
    private void initListView() {
        // Initialize the grid
        mListView = (ListView)mRootView.findViewById(R.id.list_base);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
        // set the loading and empty view container
        mLoadingEmptyContainer = (LoadingEmptyContainer)mRootView.findViewById(R.id.loading_empty_container);
        mListView.setEmptyView(mLoadingEmptyContainer);
        // Set up the helpers
        initAbsListView(mListView);
    }
}
