package be.geecko.QuickLyric.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import be.geecko.QuickLyric.MainActivity;
import be.geecko.QuickLyric.R;
import be.geecko.QuickLyric.adapter.DrawerAdapter;
import be.geecko.QuickLyric.lyrics.Lyrics;
import be.geecko.QuickLyric.tasks.SearchTask;
import be.geecko.QuickLyric.utils.OnlineAccessVerifier;

public class SearchFragment extends ListFragment {

    public boolean showTransitionAnim = true;
    private String searchQuery;
    private List<Lyrics> results;
    private boolean refresh = false;
    private ViewGroup errorView;
    private static int errorVisible = View.INVISIBLE;
    public boolean isActiveFragment = false;

    public SearchFragment() {
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        setRetainInstance(true);
        setHasOptionsMenu(false);
        super.onActivityCreated(bundle);

        ListView listView = getListView();

        if (searchQuery == null || searchQuery.equals("")) // bug
        {
            this.getFragmentManager().popBackStack();
        } else if (listView.getAdapter() == null || refresh) //refresh or empty list
        {
            refresh = false;
            search(searchQuery);
        } else if (this.results != null) //orientation change
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Lyrics l = results.get(position);
                    ((MainActivity) SearchFragment.this.getActivity()).updateLyricsFragment(R.anim.slide_out_end, l.getArtist(), l.getTrack());
                }
            });

        LayoutInflater inflater = (LayoutInflater) this.getActivity().getSystemService(MainActivity.LAYOUT_INFLATER_SERVICE);
        errorView = (ViewGroup) inflater.inflate(R.layout.error_msg, null);
        if (errorView != null) {
            errorView.setVisibility(errorVisible);
            ViewGroup layout = ((ViewGroup) ((ViewGroup) this.getView()).getChildAt(0));
            if (layout != null && errorView.getParent() == null)
                layout.addView(errorView);
            TextView txt = (TextView) errorView.getChildAt(0);
            if (txt != null)
                txt.setText(R.string.search_no_result);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (isHidden())
            return;
        DrawerAdapter drawerAdapter = ((DrawerAdapter) ((ListView) this.getActivity().findViewById(R.id.drawer_list)).getAdapter());
        this.isActiveFragment = true;
        if (drawerAdapter.getSelectedItem() != 6) {
            drawerAdapter.setSelectedItem(6);
            drawerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden)
            this.onViewCreated(getView(), null);
        else
            this.isActiveFragment = false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MainActivity mainActivity = (MainActivity) this.getActivity();
        ActionBar actionBar = mainActivity.getSupportActionBar();
        actionBar.setTitle(String.format(this.getString(R.string.search_ab_title), searchQuery));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public void refresh() {
        this.refresh = true;
    }

    public void setResults(List<Lyrics> results) {
        this.results = results;
        if (errorView != null && results.size() == 0) {
            errorView.setVisibility(View.VISIBLE);
            errorVisible = View.VISIBLE;
        }
    }

    private void search(String searchQuery) {
        this.setListShown(false);
        if (errorView != null) {
            errorView.setVisibility(View.INVISIBLE);
            errorVisible = View.INVISIBLE;
        }
        if (!OnlineAccessVerifier.check(this.getActivity())) {
            LayoutInflater inflater = (LayoutInflater) this.getActivity().getSystemService(MainActivity.LAYOUT_INFLATER_SERVICE);
            View errorView = inflater.inflate(R.layout.error_msg, this.getListView());
            Toast.makeText(this.getActivity(), this.getString(R.string.connection_error), Toast.LENGTH_LONG).show();
            ViewGroup layout = (ViewGroup) ((ViewGroup) this.getView()).getChildAt(0);
            if (layout != null && errorView != null)
                layout.addView(errorView);
        } else
            new SearchTask().execute(searchQuery, this);
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation anim = null;
        if (nextAnim != 0)
            anim = AnimationUtils.loadAnimation(getActivity(), nextAnim);
        if (anim != null)
            if (!showTransitionAnim)
                anim.setDuration(0);
            else
                showTransitionAnim = false;
        return anim;
    }

}