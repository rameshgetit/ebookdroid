package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.FileExtensionFilter;
import org.ebookdroid.utils.FileUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileListAdapter extends BaseExpandableListAdapter {

    final IBrowserActivity base;
    final AtomicBoolean inScan = new AtomicBoolean();

    private class Node {

        private final String name;
        private final String path;
        private final String[] list;

        Node(String name, String path, String[] list) {
            this.name = name;
            this.path = path;
            this.list = list;
        }

        String getName() {
            return this.name;
        }

        String getPath() {
            return this.path;
        }

        String[] getList() {
            return this.list;
        }

        int getCount() {
            return this.list.length;
        }

        public String toString() {
            return this.name;
        }
    }

    final ArrayList<Node> data = new ArrayList<Node>();

    public FileListAdapter(final IBrowserActivity base) {
        this.base = base;
    }

    @Override
    public File getChild(final int groupPosition, final int childPosition) {
        return new File(data.get(groupPosition).getPath(), data.get(groupPosition).getList()[childPosition]);
    }

    @Override
    public long getChildId(final int groupPosition, final int childPosition) {
        return childPosition;
    }

    @Override
    public int getChildrenCount(final int groupPosition) {
        return data.get(groupPosition).getCount();
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild,
            View convertView, final ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browseritem, parent, false);
        }

        final File file = getChild(groupPosition, childPosition);

        final TextView textView = (TextView) convertView.findViewById(R.id.browserItemText);
        textView.setText(file.getName());

        final ImageView imageView = (ImageView) convertView.findViewById(R.id.browserItemIcon);
        final boolean wasRead = SettingsManager.getBookSettings(file.getAbsolutePath()) != null;
        imageView.setImageResource(wasRead ? R.drawable.bookwatched : R.drawable.book);

        final TextView info = (TextView) convertView.findViewById(R.id.browserItemInfo);
        info.setText(FileUtils.getFileDate(file.lastModified()));

        final TextView fileSize = (TextView) convertView.findViewById(R.id.browserItemfileSize);
        fileSize.setText(FileUtils.getFileSize(file.length()));
        return convertView;
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, final ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browseritem, parent, false);
        }
        final Node curr = getGroup(groupPosition);

        final TextView textView = (TextView) convertView.findViewById(R.id.browserItemText);
        textView.setText(curr.getName());

        final ImageView imageView = (ImageView) convertView.findViewById(R.id.browserItemIcon);
        imageView.setImageResource(R.drawable.folderopen);

        final TextView info = (TextView) convertView.findViewById(R.id.browserItemInfo);
        info.setText("Books: " + curr.getCount());

        return convertView;
    }

    @Override
    public Node getGroup(final int groupPosition) {
        return data.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return data.size();
    }

    @Override
    public long getGroupId(final int groupPosition) {
        return groupPosition;
    }

    @Override
    public boolean isChildSelectable(final int groupPosition, final int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public void addNode(Node node) {
        if (node != null)
            data.add(node);
    }

    public void clearData() {
        data.clear();
        notifyDataSetInvalidated();
    }

    public void startScan(FileExtensionFilter filter) {
        if (inScan.compareAndSet(false, true)) {
            base.showProgress(true);
            clearData();
            new Thread(new ScanTask(filter)).start();
        }
    }

    public void stopScan() {
        inScan.set(false);
    }

    private class ScanTask implements Runnable {

        final FileExtensionFilter filter;

        final Queue<Node> currNodes = new ConcurrentLinkedQueue<Node>();

        final AtomicBoolean inUI = new AtomicBoolean();

        public ScanTask(FileExtensionFilter filter) {
            this.filter = filter;
        }

        public void run() {
            // Checks if we started to update adapter data
            if (!currNodes.isEmpty()) {
                // Add files from queue to adapter
                for (Node n = currNodes.poll(); n != null && inScan.get(); n = currNodes.poll()) {
                    addNode(n);
                }
                notifyDataSetChanged();
                // Clear flag
                inUI.set(false);
                // Finish UI part
                return;
            }

            for (String path : SettingsManager.getAppSettings().getAutoScanDirs()) {
                // Scan each valid folder
                File dir = new File(path);
                if (dir.isDirectory()) {
                    scanDir(dir);
                }
            }

            // Check if queued files are available and no UI task started
            if (!currNodes.isEmpty() && inScan.get() && inUI.compareAndSet(false, true)) {
                // Start final UI task
                base.getActivity().runOnUiThread(this);
            }
            base.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    base.showProgress(false);
                }
            });

        }

        public class DirectoryFilter implements FileFilter {

            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        }

        private void scanDir(final File dir) {
            // Checks if scan should be continued
            if (!inScan.get()) {
                return;
            }
            // Checks parameter type
            if (dir.isDirectory()) {

                final String[] list = dir.list(filter);
                if (list != null && list.length > 0) {
                    Arrays.sort(list);
                    currNodes.add(new Node(dir.getName(), dir.getAbsolutePath(), list));
                    if (inUI.compareAndSet(false, true)) {
                        // Start UI task if required
                        base.getActivity().runOnUiThread(this);
                    }
                }
                // Retrieves files from current directory
                final File[] listOfDirs = dir.listFiles(new DirectoryFilter());
                if (listOfDirs != null && inScan.get()) {
                    for (int i = 0; i < listOfDirs.length; i++) {
                        // Recursively processing found file
                        scanDir(listOfDirs[i]);
                    }
                }
            }
        }
    }

}
