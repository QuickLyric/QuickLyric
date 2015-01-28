package be.geecko.QuickLyric.tasks;

import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.view.MenuItem;
import android.widget.Toast;

import be.geecko.QuickLyric.MainActivity;
import be.geecko.QuickLyric.R;
import be.geecko.QuickLyric.fragment.LyricsViewFragment;
import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.utils.DatabaseHelper;

public class WriteToDatabaseTask extends AsyncTask<Object, Void, Boolean> {

    private Fragment fragment;
    private Context mContext;
    private MenuItem item;

    @Override
    protected Boolean doInBackground(Object... params) {
        Lyrics[] lyricses = new Lyrics[params.length - 2];
        fragment = (Fragment) params[0];
        item = (MenuItem) params[1];
        for (int i = 0; i < lyricses.length; i++) {
            lyricses[i] = (Lyrics) params[i + 2];
        }
        mContext = fragment.getActivity();
        String table = "lyrics";
        SQLiteDatabase database = ((MainActivity) mContext).database;
        Boolean result = false;
        String[] columns = DatabaseHelper.columns;
        if (database != null) {
            for (Lyrics lyrics : lyricses)
                if (!DatabaseHelper.presenceCheck(database, new String[]{lyrics.getArtist(), lyrics.getTrack()})) {
                    ContentValues values = new ContentValues(2);
                    values.put(columns[0], lyrics.getArtist());
                    values.put(columns[1], lyrics.getTrack());
                    values.put(columns[2], lyrics.getText());
                    values.put(columns[3], lyrics.getURL());
                    values.put(columns[4], lyrics.getSource());
                    values.put(columns[5], lyrics.getCoverURL());
                    database.insert(table, null, values);
                    if (fragment instanceof LyricsViewFragment)
                        ((LyricsViewFragment) fragment).lyricsPresentInDB = true;
                    result = true;
                } else {
                    database.delete(table, String.format("%s=? AND %s=?", columns[0], columns[1]), new String[]{lyrics.getArtist(), lyrics.getTrack()});
                    if (fragment instanceof LyricsViewFragment)
                        ((LyricsViewFragment) fragment).lyricsPresentInDB = false;
                    result = false;
                }
        }
        return result;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (fragment instanceof LyricsViewFragment) {
            int message = result ? R.string.lyrics_saved : R.string.lyrics_removed;
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            item.setIcon(result ? R.drawable.ic_trash : R.drawable.ic_save);
            item.setTitle(result ? R.string.remove_action : R.string.save_action);
        } else
            new DBContentLister().execute(fragment);
    }
}