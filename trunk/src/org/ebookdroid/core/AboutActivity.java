package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.utils.LengthUtils;
import org.ebookdroid.utils.Wiki;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class AboutActivity extends Activity {

    private static final Part[] PARTS = {
            // Start
            new Part(R.string.about_commmon_title, Format.HTML, "about_common.html"),
            new Part(R.string.about_license_title, Format.HTML, "about_license.html"),
            new Part(R.string.about_3dparty_title, Format.HTML, "about_3rdparty.html"),
            new Part(R.string.about_changelog_title, Format.WIKI, "about_changelog.wiki"),
            new Part(R.string.about_thanks_title, Format.HTML, "about_thanks.html"),
    // End
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.about);

        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

        String name = "EBookDroid";
        String version = "";
        try {
            final PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = packageInfo.versionName;
            name = getResources().getString(packageInfo.applicationInfo.labelRes);
        } catch (final NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final TextView title = (TextView) findViewById(R.id.about_title);
        title.setText(name + (LengthUtils.isNotEmpty(version) ? " v" + version : ""));

        final ExpandableListView view = (ExpandableListView) findViewById(R.id.about_parts);
        view.setAdapter(new PartsAdapter());
        view.expandGroup(0);
    }

    private static class Part {

        final int labelId;
        final Format format;
        final String fileName;
        CharSequence content;

        public Part(final int labelId, final Format format, final String fileName) {
            this.labelId = labelId;
            this.format = format;
            this.fileName = fileName;
        }

        public CharSequence getContent(final Context context) {
            if (content == null) {
                try {
                    final InputStream input = context.getAssets().open(fileName);
                    final int size = input.available();
                    byte[] buffer = new byte[size];
                    input.read(buffer);
                    input.close();
                    final String text = new String(buffer, "UTF8");
                    content = format.format(text);
                } catch (final IOException e) {
                    e.printStackTrace();
                    content = "";
                }
            }
            return content;
        }
    }

    public class PartsAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return PARTS.length;
        }

        @Override
        public int getChildrenCount(final int groupPosition) {
            return 1;
        }

        @Override
        public Part getGroup(final int groupPosition) {
            return PARTS[groupPosition];
        }

        @Override
        public Part getChild(final int groupPosition, final int childPosition) {
            return PARTS[groupPosition];
        }

        @Override
        public long getGroupId(final int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(final int groupPosition, final int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView,
                final ViewGroup parent) {
            View container = null;
            TextView view = null;
            if (convertView == null) {
                container = LayoutInflater.from(AboutActivity.this).inflate(R.layout.about_part, parent, false);
            } else {
                container = convertView;
            }
            view = (TextView) container.findViewById(R.id.about_partText);
            view.setText(getGroup(groupPosition).labelId);
            return container;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild,
                final View convertView, final ViewGroup parent) {
            WebView view = null;
            if (!(convertView instanceof WebView)) {
                view = new WebView(AboutActivity.this);
                view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.WRAP_CONTENT, AbsListView.LayoutParams.WRAP_CONTENT));
            } else {
                view = ((WebView) convertView);
            }
            CharSequence content = getChild(groupPosition, childPosition).getContent(AboutActivity.this);
            view.setBackgroundColor(Color.GRAY);
            view.loadDataWithBaseURL("file:///fake/not_used", content.toString(), "text/html", "UTF-8", "");
            return view;
        }

        @Override
        public boolean isChildSelectable(final int groupPosition, final int childPosition) {
            return false;
        }
    }

    private static enum Format {
        /**
         *
         */
        TEXT,

        /**
         *
         */
        HTML,

        /**
         *
         */
        WIKI {

            @Override
            public CharSequence format(final String text) {
                return Wiki.fromWiki(text);
            }
        };

        public CharSequence format(final String text) {
            return text;
        }
    }
}
