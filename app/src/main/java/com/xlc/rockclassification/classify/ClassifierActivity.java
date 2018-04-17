/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xlc.rockclassification.classify;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;

import com.xlc.rockclassification.RockApplication;
import com.xlc.rockclassification.classify.OverlayView.DrawCallback;
import com.xlc.rockclassification.classify.env.BorderedText;
import com.xlc.rockclassification.classify.env.ImageUtils;
import com.xlc.rockclassification.classify.env.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;

import com.xlc.rockclassification.R;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    protected static final boolean SAVE_PREVIEW_BITMAP = false;

    private ResultsView resultsView;

    private RecyclerView ejrecyclerView = null;
    private RecyclerView sjrecyclerView = null;
    private MyAdapter ejadapter = null;
    private MyAdapter sjadapter = null;
    private boolean isFirst = true;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private long lastProcessingTimeMs;

    // These are the settings for the original v1 Inception model. If you want to
    // use a model that's been produced from the TensorFlow for Poets codelab,
    // you'll need to set IMAGE_SIZE = 299, IMAGE_MEAN = 128, IMAGE_STD = 128,
    // INPUT_NAME = "Mul", and OUTPUT_NAME = "final_result".
    // You'll also need to update the MODEL_FILE and LABEL_FILE paths to point to
    // the ones you produced.
    //
    // To use v3 Inception model, strip the DecodeJpeg Op from your retrained
    // model first:
    //
    // python strip_unused.py \
    // --input_graph=<retrained-pb-file> \
    // --output_graph=<your-stripped-pb-file> \
    // --input_node_names="Mul" \
    // --output_node_names="final_result" \
    // --input_binary=true


    private static final boolean MAINTAIN_ASPECT = true;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(1600, 1200);


    private Integer sensorOrientation;
    private Classifier classifier;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;


    private BorderedText borderedText;


    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    private static final float TEXT_SIZE_DIP = 10;

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        classifier = RockApplication.getClassifier(this);

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(RockApplication.INPUT_SIZE, RockApplication.INPUT_SIZE, Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(
                previewWidth, previewHeight,
                RockApplication.INPUT_SIZE, RockApplication.INPUT_SIZE,
                sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
        addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        renderDebug(canvas);
                    }
                });
    }

    @Override
    protected void processImage() {
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }
        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                        //LOGGER.i("Detect: %s", results);
                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        Message msg = Message.obtain();
                        msg.arg1 = 0;
                        msg.obj = results;
                        resultUiHandler.sendMessage(msg);
                        requestRender();
                        readyForNextImage();
                    }
                });
    }

    public Handler resultUiHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.arg1) {
                case 0:
                    List<Classifier.Recognition> results = (List<Classifier.Recognition>) msg.obj;
                    if(isFirst){
                        isFirst = false;
                        initResultsView(results);
                    }else {
                        updateResultsView(results);
                    }
                    break;
            }
            return false;
        }
    });


    public void initResultsView(final List<Classifier.Recognition> results) {

        if (ejrecyclerView == null) {
            ejrecyclerView = findViewById(R.id.ejresults);
        }
        if (sjrecyclerView == null) {
            sjrecyclerView = findViewById(R.id.sjresults);
        }

        LinearLayoutManager ejlayoutmanager = new LinearLayoutManager(this);
        ejadapter = new MyAdapter(RockApplication.getErjResults(results), true);
        ejrecyclerView.setLayoutManager(ejlayoutmanager);
        ejrecyclerView.setAdapter(ejadapter);
        DividerItemDecoration ejitemDecoration = new DividerItemDecoration(this,ejlayoutmanager.getOrientation());
        ejrecyclerView.addItemDecoration(ejitemDecoration);


        LinearLayoutManager sjlayoutmanager = new LinearLayoutManager(this);
        sjadapter = new MyAdapter(results, false);
        sjrecyclerView.setLayoutManager(sjlayoutmanager);
        sjrecyclerView.setAdapter(sjadapter);
        DividerItemDecoration sjitemDecoration = new DividerItemDecoration(this,sjlayoutmanager.getOrientation());
        sjrecyclerView.addItemDecoration(sjitemDecoration);

        ejadapter.setOnItemClickListener(new MyAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                String title = ejadapter.getCurrentTitle(position);
                if(title!=null){
                    sjadapter.updateData(RockApplication.getSjResultsFromEjTitle(results,title));
                }
            }
        });

    }

    public void updateResultsView(List<Classifier.Recognition> results) {
        ejadapter.updateData(RockApplication.getErjResults(results));
        sjadapter.updateData(results);
    }



    @Override
    public void onSetDebug(boolean debug) {
        classifier.enableStatLogging(debug);
    }

    private void renderDebug(final Canvas canvas) {
        if (!isDebug()) {
            return;
        }
        final Bitmap copy = cropCopyBitmap;
        if (copy != null) {
            final Matrix matrix = new Matrix();
            final float scaleFactor = 2;
            matrix.postScale(scaleFactor, scaleFactor);
            matrix.postTranslate(
                    canvas.getWidth() - copy.getWidth() * scaleFactor,
                    canvas.getHeight() - copy.getHeight() * scaleFactor);
            canvas.drawBitmap(copy, matrix, new Paint());

            final Vector<String> lines = new Vector<String>();
            if (classifier != null) {
                String statString = classifier.getStatString();
                String[] statLines = statString.split("\n");
                for (String line : statLines) {
                    lines.add(line);
                }
            }

            lines.add("Frame: " + previewWidth + "x" + previewHeight);
            lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
            lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
            lines.add("Rotation: " + sensorOrientation);
            lines.add("Inference time: " + lastProcessingTimeMs + "ms");

            borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
        }
    }
}
