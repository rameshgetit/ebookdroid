package org.ebookdroid.ui.library.tasks;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.ui.library.adapters.BookNode;
import org.ebookdroid.ui.library.adapters.RecentAdapter;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.emdev.ui.progress.UIFileCopying;
import org.emdev.ui.tasks.BaseFileAsyncTask;

public class CopyBookTask extends BaseFileAsyncTask<BookNode> {

    private final RecentAdapter recentAdapter;
    private final File targetFolder;

    private BookNode book;
    private File origin;

    public CopyBookTask(final Context context, final RecentAdapter recentAdapter, final File targetFolder) {
        super(context, R.string.book_copy_start, R.string.book_copy_complete, R.string.book_copy_error, false);
        this.recentAdapter = recentAdapter;
        this.targetFolder = targetFolder;
    }

    @Override
    protected FileTaskResult doInBackground(final BookNode... params) {
        book = params[0];
        origin = new File(book.path);
        final File target = new File(targetFolder, origin.getName());
        try {

            final UIFileCopying worker = new UIFileCopying(R.string.book_copy_progress, 256 * 1024, this);
            final BufferedInputStream in = new BufferedInputStream(new FileInputStream(origin), 256 * 1024);
            final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target), 256 * 1024);

            worker.copy(origin.length(), in, out);

            return new FileTaskResult(target);
        } catch (final IOException ex) {
            return new FileTaskResult(ex);
        } catch (final Throwable th) {
            th.printStackTrace();
        }

        return null;
    }

    @Override
    protected void processTargetFile(final File target) {
        if (book.settings != null) {
            try {
                final BookSettings bs = SettingsManager.copyBookSettings(target, book.settings);
                if (recentAdapter != null) {
                    recentAdapter.replaceBook(null, bs);
                }
            } catch (final Throwable th) {
                th.printStackTrace();
            }
        }
        super.processTargetFile(target);
    }
}
