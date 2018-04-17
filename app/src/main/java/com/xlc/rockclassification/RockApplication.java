package com.xlc.rockclassification;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.xlc.rockclassification.classify.Classifier;
import com.xlc.rockclassification.classify.TensorFlowImageClassifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class RockApplication extends Application{

    public static final int INPUT_SIZE = 500;
    public static final int IMAGE_MEAN = 117;
    public static final float IMAGE_STD = 1;
    public static final String INPUT_NAME = "input_images";
    public static final String OUTPUT_NAME = "output";


    public static final String MODEL_FILE = "file:///android_asset/frozen9lite.pb";
    public static final String LABEL_FILE = "file:///android_asset/stone_graph_label_strings.txt";

    public static volatile Classifier classifier = null;

    public static Classifier getClassifier(Context context){
        if(classifier==null){
            synchronized (Application.class){
                classifier = TensorFlowImageClassifier.create(
                        context.getAssets(),
                        MODEL_FILE,
                        LABEL_FILE,
                        INPUT_SIZE,
                        IMAGE_MEAN,
                        IMAGE_STD,
                        INPUT_NAME,
                        OUTPUT_NAME);
            }
        }
        return classifier;
    }

    public static List<Classifier.Recognition> getSjResultsFromEjTitle(final List<Classifier.Recognition> results, final String title){
        List<Classifier.Recognition> sjResults = new ArrayList<>();
        for(Classifier.Recognition recognition:results){
            if(recognition.getTitle().split("-")[0].equals(title)){
                sjResults.add(recognition);
            }
        }
        return sjResults;
    }

    public static List<Classifier.Recognition> getErjResults(List<Classifier.Recognition> sjresults) {
        Log.i("sjtitle", sjresults.size()+ ":" + sjresults);
        List<Classifier.Recognition> ejresults = new ArrayList<>();
        HashMap<String, Float> resultsmap = new HashMap();
        for (Classifier.Recognition recognition : sjresults) {
            String title = recognition.getTitle().split("-")[0];
            float conf = 0;
            if (resultsmap.containsKey(title)) {
                conf = recognition.getConfidence() + resultsmap.get(title);
            } else {
                conf = recognition.getConfidence();
            }
            resultsmap.put(title, conf);
        }
        PriorityQueue<Classifier.Recognition> pq =
                new PriorityQueue<Classifier.Recognition>(
                        sjresults.size(),
                        new Comparator<Classifier.Recognition>() {
                            @Override
                            public int compare(Classifier.Recognition lhs, Classifier.Recognition rhs) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        Iterator iter = resultsmap.entrySet().iterator();
        int index = 0;
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String title = (String) entry.getKey();
            Float val = (Float) entry.getValue();
            pq.add(new Classifier.Recognition("" + index, title, val, null));
            index++;
        }
        int length = pq.size();
        for (int i = 0; i < length; ++i) {
            ejresults.add(pq.poll());
        }
        Log.i("ejtitle", ejresults.size()+ ":" + ejresults);
        pq.clear();
        return ejresults;
    }




}
