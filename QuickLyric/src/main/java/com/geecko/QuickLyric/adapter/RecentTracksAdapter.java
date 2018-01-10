package com.geecko.QuickLyric.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.geecko.QuickLyric.MainActivity;
import com.geecko.QuickLyric.R;
import com.geecko.QuickLyric.event.RecentsDownloadingEvent;
import com.geecko.QuickLyric.model.Recents;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

/**
 * Created by steve on 4/7/17.
 */

public class RecentTracksAdapter extends RecyclerView.Adapter<RecentTracksAdapter.ViewHolder> {

    private final Context mContext;
    private Recents mData;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView mArtistTextView;
        public TextView mTitleTextView;
        public ImageView mImageView;
        public FrameLayout mContainer;
        public ViewHolder(View v) {
            super(v);
            mArtistTextView = v.findViewById(R.id.recents_artist_text);
            mTitleTextView = v.findViewById(R.id.recents_title_text);
            mImageView = v.findViewById(R.id.recents_image_view_cover_art);
            mContainer = v.findViewById(R.id.item_container);
        }
    }

    public RecentTracksAdapter(Context context) {
        mContext = context.getApplicationContext();
        mData = Recents.getInstance(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {

        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View v = inflater.inflate(R.layout.list_item_track, parent, false);
        return new ViewHolder(v);

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        String artist = mData.get(position).mArtist;
        String title = mData.get(position).mTitle;

        holder.mArtistTextView.setText(artist);
        holder.mTitleTextView.setText(title);

        File artworksDir = new File(mContext.getCacheDir(), "artworks");
        File artworkFile = new File(artworksDir, artist + title + ".png");

        holder.mContainer.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            String artist1 = mData.get(pos).mArtist;
            String title1 = mData.get(pos).mTitle;
            ((MainActivity) holder.mArtistTextView.getContext()).updateLyricsFragment(0, artist1, title1);
            EventBus.getDefault().post(new RecentsDownloadingEvent());
        });

        Glide.with(mContext)
                .load(artworkFile)
                .centerCrop()
                .crossFade()
                .fallback(R.drawable.no_cover)
                .placeholder(R.drawable.no_cover)
                .into(holder.mImageView);
    }

    @Override
    public int getItemCount() {
        return Recents.getInstance(mContext).size();
    }
}



