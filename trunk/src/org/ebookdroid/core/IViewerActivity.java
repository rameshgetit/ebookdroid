package org.ebookdroid.core;

import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.core.touch.IMultiTouchZoom;

import android.app.Activity;
import android.content.Context;

public interface IViewerActivity {

    Context getContext();

    Activity getActivity();

    DecodeService getDecodeService();

    DocumentModel getDocumentModel();

    BaseDocumentView getView();

    IDocumentViewController getDocumentController();

    ZoomModel getZoomModel();

    DecodingProgressModel getDecodingProgressModel();

    void switchDocumentController();
}
