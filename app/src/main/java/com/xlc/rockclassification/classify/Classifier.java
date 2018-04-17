/* Copyright 2015 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.xlc.rockclassification.classify;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Locale;

/**
 * Generic interface for interacting with different recognition engines.
 */
public interface Classifier {
  /**
   * An immutable result returned by a Classifier describing what was recognized.
   */
  class Recognition implements Parcelable{
    /**
     * A unique identifier for what has been recognized. Specific to the class, not the instance of
     * the object.
     */
    private final String id;

    /**
     * Display name for the recognition.
     */
    private final String title;

    /**
     * A sortable score for how good the recognition is relative to others. Higher should be better.
     */
    private final float confidence;

    /** Optional location within the source image for the location of the recognized object. */
    private RectF location;

    public Recognition(final String id, final String title, final Float confidence, final RectF location) {
      this.id = id;
      this.title = title;
      this.confidence = confidence;
      this.location = location;
    }

    private Recognition(Parcel in) {
      id = in.readString();
      title = in.readString();
      confidence = in.readFloat();
      location = in.readParcelable(RectF.class.getClassLoader());
    }

    public static final Creator<Recognition> CREATOR = new Creator<Recognition>() {
      @Override
      public Recognition createFromParcel(Parcel in) {
        return new Recognition(in);
      }

      @Override
      public Recognition[] newArray(int size) {
        return new Recognition[size];
      }
    };

    public String getId() {
      return id;
    }

    public String getTitle() {
      return title;
    }

    public Float getConfidence() {
      return confidence;
    }

    public RectF getLocation() {
      return new RectF(location);
    }

    public void setLocation(RectF location) {
      this.location = location;
    }

    @Override
    public String toString() {
      String resultString = "";
      if (id != null) {
        resultString += "[" + id + "] ";
      }

      if (title != null) {
        resultString += title + " ";
      }

      resultString += String.format(Locale.getDefault(),"(%.1f%%)",confidence * 100.0f);//("(%.1f%%) ", confidence * 100.0f);
      /*if (confidence != null) {
        resultString += String.format("(%.1f%%) ", confidence * 100.0f);
      }*/

      if (location != null) {
        resultString += location + " ";
      }

      return resultString.trim();
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(id);
      dest.writeString(title);
      dest.writeFloat(confidence);
      dest.writeParcelable(location, flags);
    }
  }

  List<Recognition> recognizeImage(Bitmap bitmap);

  void enableStatLogging(final boolean debug);

  String getStatString();

  void close();
}
