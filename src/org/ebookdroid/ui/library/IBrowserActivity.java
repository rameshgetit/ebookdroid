package org.ebookdroid.ui.library;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import java.io.File;

import org.emdev.common.filesystem.FileSystemScanner;

public interface IBrowserActivity extends FileSystemScanner.ProgressListener {

    Context getContext();

    Activity getActivity();

    void setCurrentDir(File newDir);

    void showDocument(Uri uri);

    void loadThumbnail(String path, ImageView imageView, int defaultResID);
}
