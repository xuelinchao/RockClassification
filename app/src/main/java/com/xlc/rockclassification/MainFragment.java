package com.xlc.rockclassification;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.xlc.rockclassification.classify.Classifier;
import com.xlc.rockclassification.classify.MyAdapter;
import com.xlc.rockclassification.classify.env.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


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

    private RecyclerView ejrecyclerView = null;
    private RecyclerView sjrecyclerView = null;
    private MyAdapter ejadapter = null;
    private MyAdapter sjadapter = null;

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
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        imageView = view.findViewById(R.id.rock_imgview);

        ejrecyclerView = view.findViewById(R.id.main_ejresults);
        sjrecyclerView = view.findViewById(R.id.main_sjresults);

        final List<Classifier.Recognition> results = new ArrayList<>();
        initResultsView(results);

        classifier = RockApplication.getClassifier(getContext());

        FloatingActionButton floatingActionButton = view.findViewById(R.id.select_img);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getContext(),"test",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
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
                    updateResultsView(results);
                    break;
                case 1:
                    final Uri muri = (Uri) msg.obj;
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap bitmap = null;
                            try {
                                bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), muri);
                                Bitmap croppedBitmap = ThumbnailUtils.extractThumbnail(bitmap, RockApplication.INPUT_SIZE, RockApplication.INPUT_SIZE);
                                final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
                                Message msg = Message.obtain();
                                msg.arg1 = 0;
                                msg.obj = results;
                                resultUiHandler.sendMessage(msg);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
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
        DividerItemDecoration ejitemDecoration = new DividerItemDecoration(getContext(), ejlayoutmanager.getOrientation());
        ejrecyclerView.addItemDecoration(ejitemDecoration);


        LinearLayoutManager sjlayoutmanager = new LinearLayoutManager(getContext());
        sjadapter = new MyAdapter(results, false);
        sjrecyclerView.setLayoutManager(sjlayoutmanager);
        sjrecyclerView.setAdapter(sjadapter);
        DividerItemDecoration sjitemDecoration = new DividerItemDecoration(getContext(), sjlayoutmanager.getOrientation());
        sjrecyclerView.addItemDecoration(sjitemDecoration);
    }

    public void updateResultsView(final List<Classifier.Recognition> results) {
        ejadapter.updateData(RockApplication.getErjResults(results));
        sjadapter.updateData(results);

        ejadapter.setOnItemClickListener(new MyAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                String title = ejadapter.getCurrentTitle(position);
                if (title != null) {
                    sjadapter.updateData(RockApplication.getSjResultsFromEjTitle(results, title));
                }
            }
        });
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (resultCode) {
            case -1:
                if(requestCode==1){
                    //Toast.makeText(getContext(),"onActivityResult1",Toast.LENGTH_SHORT).show();
                    final Uri uri = data.getData();
                    //imageChanged(uri);
                    imageView.setImageURI(uri);
                    //Toast.makeText(getContext(),"onActivityResult2",Toast.LENGTH_SHORT).show();
                    Message msg = Message.obtain();
                    msg.arg1 = 1;
                    msg.obj = uri;
                    resultUiHandler.sendMessage(msg);
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
