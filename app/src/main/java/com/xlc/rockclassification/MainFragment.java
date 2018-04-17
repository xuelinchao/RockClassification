package com.xlc.rockclassification;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.xlc.rockclassification.classify.Classifier;
import com.xlc.rockclassification.classify.MyAdapter;

import java.util.List;

/**
 * A fragment with a Google +1 button.
 * Activities that contain this fragment must implement the
 * {@link MainFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Handler handler;
    private HandlerThread handlerThread;
    private ImageView imageView;

    //private OnFragmentInteractionListener mListener;

    private RecyclerView ejrecyclerView = null;
    private RecyclerView sjrecyclerView = null;
    private MyAdapter ejadapter = null;
    private MyAdapter sjadapter = null;
    private boolean isFirst = true;

    private Classifier classifier;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        imageView = view.findViewById(R.id.rock_imgview);

        ejrecyclerView = view.findViewById(R.id.main_ejresults);
        sjrecyclerView = view.findViewById(R.id.main_sjresults);

        classifier = RockApplication.getClassifier(getContext());

        FloatingActionButton floatingActionButton = view.findViewById(R.id.select_img);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getContext(),"test",Toast.LENGTH_SHORT).show();
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap croppedBitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                        croppedBitmap = ThumbnailUtils.extractThumbnail(croppedBitmap,RockApplication.INPUT_SIZE,RockApplication.INPUT_SIZE);
                        final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
                        Message msg = Message.obtain();
                        msg.arg1 = 0;
                        msg.obj = results;
                        resultUiHandler.sendMessage(msg);
                    }
                });
            }
        });

        return view;
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

        LinearLayoutManager ejlayoutmanager = new LinearLayoutManager(getContext());
        ejadapter = new MyAdapter(RockApplication.getErjResults(results), true);
        ejrecyclerView.setLayoutManager(ejlayoutmanager);
        ejrecyclerView.setAdapter(ejadapter);
        DividerItemDecoration ejitemDecoration = new DividerItemDecoration(getContext(),ejlayoutmanager.getOrientation());
        ejrecyclerView.addItemDecoration(ejitemDecoration);


        LinearLayoutManager sjlayoutmanager = new LinearLayoutManager(getContext());
        sjadapter = new MyAdapter(results, false);
        sjrecyclerView.setLayoutManager(sjlayoutmanager);
        sjrecyclerView.setAdapter(sjadapter);
        DividerItemDecoration sjitemDecoration = new DividerItemDecoration(getContext(),sjlayoutmanager.getOrientation());
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

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onPause() {

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            //LOGGER.e(e, "Exception!");
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        handlerThread = new HandlerThread("inference2");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /*@Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    *//**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     *//*
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }*/

}
