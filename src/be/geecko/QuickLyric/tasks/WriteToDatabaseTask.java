package be.geecko.QuickLyric.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import be.geecko.QuickLyric.MainActivity;
import be.geecko.QuickLyric.R;
import be.geecko.QuickLyric.fragment.LyricsViewFragment;
import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.utils.DatabaseHelper;

public class WriteToDatabaseTask extends AsyncTask<Object, Void, Integer> {

    private Fragment fragment;
    private Context mContext;

    @Override
    protected Integer doInBackground(Object... params) {
        Lyrics[] lyricses = new Lyrics[params.length - 1];
        fragment = (Fragment) params[0];
        for (int i = 0; i < lyricses.length; i++) {
            lyricses[i] = (Lyrics) params[++i];
        }
        mContext = fragment.getActivity();
        String table = "lyrics";
        SQLiteDatabase database = ((MainActivity) mContext).database;
        Integer message = 0;
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
                    message = R.string.lyrics_saved;
                } else {
                    database.delete(table, String.format("%s=? AND %s=?", columns[0], columns[1]), new String[]{lyrics.getArtist(), lyrics.getTrack()});
                    if (fragment instanceof LyricsViewFragment)
                        ((LyricsViewFragment) fragment).lyricsPresentInDB = false;
                    message = R.string.lyrics_removed;
                }
        }
        return message;
    }

    @Override
    protected void onPostExecute(Integer message) {
        if (fragment instanceof LyricsViewFragment) {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            ((MainActivity) mContext).supportInvalidateOptionsMenu();
        } else {
            new DBContentLister().execute(fragment);
        }
    }
}
