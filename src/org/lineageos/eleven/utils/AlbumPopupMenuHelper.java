/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.lineageos.eleven.utils;

import android.app.Activity;
import android.provider.MediaStore;
import androidx.fragment.app.FragmentManager;

import android.view.MenuItem;
import org.lineageos.eleven.Config;
import org.lineageos.eleven.cache.ImageFetcher;
import org.lineageos.eleven.menu.DeleteDialog;
import org.lineageos.eleven.menu.FragmentMenuItems;
import org.lineageos.eleven.menu.PhotoSelectionDialog;
import org.lineageos.eleven.model.Album;

import java.util.TreeSet;

public abstract class AlbumPopupMenuHelper extends PopupMenuHelper {
    protected Album mAlbum;

    public AlbumPopupMenuHelper(Activity activity, FragmentManager fragmentManager) {
        super(activity, fragmentManager);
        mType = PopupMenuType.Album;
    }

    public abstract Album getAlbum(int position);

    @Override
    public PopupMenuType onPreparePopupMenu(int position) {
        mAlbum = getAlbum(position);

        if (mAlbum == null) {
            return null;
        }

        return PopupMenuType.Album;
    }

    @Override
    protected long[] getIdList() {
        return MusicUtils.getSongListForAlbum(mActivity, mAlbum.mAlbumId);
    }

    @Override
    protected long getSourceId() {
        return mAlbum.mAlbumId;
    }

    @Override
    protected Config.IdType getSourceType() {
        return Config.IdType.Album;
    }

    @Override
    protected void onDeleteClicked() {
        final String album = mAlbum.mAlbumName;
        DeleteDialog.newInstance(album, getIdList(),
                ImageFetcher.generateAlbumCacheKey(album, mAlbum.mArtistName))
                .show(mFragmentManager, "DeleteDialog");
    }

    @Override
    protected String getArtistName() {
        return mAlbum.mArtistName;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        boolean handled = super.onMenuItemClick(item);
        if (!handled && item.getGroupId() == getGroupId()) {
            switch (item.getItemId()) {
                case FragmentMenuItems.CHANGE_IMAGE:
                    String key = ImageFetcher.generateAlbumCacheKey(mAlbum.mAlbumName,
                            getArtistName());
                    PhotoSelectionDialog.newInstance(mAlbum.mAlbumName,
                            PhotoSelectionDialog.ProfileType.ALBUM, key)
                            .show(mFragmentManager, "PhotoSelectionDialog");
                    return true;
            }
        }

        return handled;
    }

    @Override
    protected void updateMenuIds(PopupMenuType type, TreeSet<Integer> set) {
        super.updateMenuIds(type, set);

        // Don't show more by artist if it is an unknown artist
        if (MediaStore.UNKNOWN_STRING.equals(mAlbum.mArtistName)) {
            set.remove(FragmentMenuItems.MORE_BY_ARTIST);
        }
    }
}
