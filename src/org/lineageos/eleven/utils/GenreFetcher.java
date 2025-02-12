package org.lineageos.eleven.utils;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.view.View;
import android.widget.TextView;

public class GenreFetcher implements LoaderCallbacks<Cursor> {
    private static final String[] GENRE_PROJECTION = new String[] { MediaStore.Audio.Genres.NAME };

    private Context mContext;
    private int mSongId;
    private TextView mTextView;

    public static void fetch(FragmentActivity activity, int songId, TextView textView) {
        LoaderManager lm = activity.getSupportLoaderManager();
        lm.restartLoader(0, null, new GenreFetcher(activity, songId, textView));
    }

    private GenreFetcher(Context context, int songId, TextView textView) {
        mContext = context;
        mSongId = songId;
        mTextView = textView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mContext,
            MediaStore.Audio.Genres.getContentUriForAudioId("external", mSongId),
            GENRE_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if(mTextView != null && cursor.moveToFirst()) {
            String genre = cursor.getString(0);
            if(!MusicUtils.isBlank(genre)) {
                mTextView.setText(genre);
                mTextView.setVisibility(View.VISIBLE);
                return;
            }
        }
        // no displayable genre found
        mTextView.setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}
}
