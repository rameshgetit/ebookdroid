package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.presentation.BooksAdapter.ViewHolder;
import org.ebookdroid.utils.StringUtils;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class BookShelfAdapter extends BaseAdapter {

    private final IBrowserActivity base;
    private final IdentityHashMap<DataSetObserver, DataSetObserver> observers = new IdentityHashMap<DataSetObserver, DataSetObserver>();

    final int id;
    final String name;
    final String path;

    final List<BookNode> nodes = new ArrayList<BookNode>();

    public BookShelfAdapter(final IBrowserActivity base, final int index, final String name, final String path) {
        this.base = base;
        this.id = index;
        this.name = name;
        this.path = path;
    }

    @Override
    public int getCount() {
        return nodes.size();
    }

    @Override
    public Object getItem(final int position) {
        return nodes.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, final View view, final ViewGroup parent) {
        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.thumbnail, view,
                parent);

        final BookNode node = nodes.get(position);

        holder.textView.setText(StringUtils.cleanupTitle(node.name));
        base.loadThumbnail(node.path, holder.imageView, R.drawable.book);

        return holder.getView();
    }

    public String getPath() {
        return path;
    }

    @Override
    public void registerDataSetObserver(final DataSetObserver observer) {
        if (!observers.containsKey(observer)) {
            super.registerDataSetObserver(observer);
            observers.put(observer, observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(final DataSetObserver observer) {
        if (null != observers.remove(observer)) {
            super.unregisterDataSetObserver(observer);
        }
    }
}
