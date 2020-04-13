package fr.gaulupeau.apps.Poche.data;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.List;

import fr.gaulupeau.apps.InThePoche.R;
import fr.gaulupeau.apps.Poche.data.dao.entities.Article;

import static fr.gaulupeau.apps.Poche.data.ListTypes.*;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private Context context;
    private Settings settings;

    private List<Article> articles;
    private OnItemClickListener listener;
    private int listType = -1;

    public ListAdapter(Context context, Settings settings,
                       List<Article> articles, OnItemClickListener listener, int listType) {
        this.context = context;
        this.settings = settings;
        this.articles = articles;
        this.listener = listener;
        this.listType = listType;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(articles.get(position));
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        OnItemClickListener listener;
        TextView title;
        TextView url;
        ImageView favourite;
        ImageView read;
        TextView readingTime;
        ImageView preview;

        public ViewHolder(View itemView, OnItemClickListener listener) {
            super(itemView);
            this.listener = listener;
            title = (TextView) itemView.findViewById(R.id.title);
            url = (TextView) itemView.findViewById(R.id.url);
            favourite = (ImageView) itemView.findViewById(R.id.favourite);
            read = (ImageView) itemView.findViewById(R.id.read);
            readingTime = (TextView) itemView.findViewById(R.id.estimatedReadingTime);
            preview = (ImageView) itemView.findViewById(R.id.previewImage);
            itemView.setOnClickListener(this);
        }

        public void bind(Article article) {
            title.setText(article.getTitle());
            url.setText(article.getDomain());

            boolean showFavourite = false;
            boolean showRead = false;
            switch(listType) {
                case LIST_TYPE_UNREAD:
                case LIST_TYPE_ARCHIVED:
                    showFavourite = article.getFavorite();
                    break;

                case LIST_TYPE_FAVORITES:
                    showRead = article.getArchive();
                    break;

                default: // we don't actually use it right now
                    showFavourite = article.getFavorite();
                    showRead = article.getArchive();
                    break;
            }
            //favourite.setVisibility(showFavourite ? View.VISIBLE : View.GONE);
            //read.setVisibility(showRead ? View.VISIBLE : View.GONE);
            favourite.setVisibility(View.GONE);
            read.setVisibility(View.GONE);
            readingTime.setText(context.getString(R.string.listItem_estimatedReadingTime,
                    article.getEstimatedReadingTime(settings.getReadingSpeed())));

            if (settings.isPreviewImageEnabled() && !TextUtils.isEmpty(article.getPreviewPictureURL())) {
                new DownloadImageTask(preview).execute(article.getPreviewPictureURL());
                preview.setVisibility(View.VISIBLE);
            }
            else {
                preview.setVisibility(View.GONE);
            }

        }

        @Override
        public void onClick(View v) {
            listener.onItemClick(getAdapterPosition());
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}
