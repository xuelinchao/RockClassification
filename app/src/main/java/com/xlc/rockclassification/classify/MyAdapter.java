package com.xlc.rockclassification.classify;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xlc.rockclassification.R;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private static final int SCORE_FORMAT = 1000;

    private boolean ejresult;
    private List<Classifier.Recognition> mData;

    private OnItemClickListener mOnItemClickListener;

    public MyAdapter(List<Classifier.Recognition> Data,boolean ej){
        this.mData = Data;
        this.ejresult = ej;
    }

    public void updateData(List<Classifier.Recognition> Data){
        this.mData = Data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_results_item,parent,false);
        //ViewHolder viewHolder = new ViewHolder(v);
        return new ViewHolder(v);
    }

    private String getConfidence(float con){
        return String.valueOf((float)(Math.round(con*SCORE_FORMAT))/SCORE_FORMAT);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final int pos = position;
        Classifier.Recognition recognition = mData.get(pos);
        if(recognition!=null){

            if(ejresult){
                holder.result.setText(recognition.getTitle());
                holder.confidence.setText(getConfidence(recognition.getConfidence()));
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnItemClickListener.onClick(pos);
                    }
                });
            }else {
                //holder.results.setText(recognition.getTitle().split("-")[1] + ": " + recognition.getConfidence());
                holder.result.setText(recognition.getTitle().split("-")[1]);
                holder.confidence.setText(getConfidence(recognition.getConfidence()));
            }
        }
    }

    @Override
    public int getItemCount() {
        return mData==null ? 0 : mData.size();
    }

    public synchronized String getCurrentTitle(int position){
        if(position>=0&&position<mData.size()){
            return mData.get(position).getTitle();
        }
        return null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        TextView result;
        TextView confidence;

        public ViewHolder(View itemView) {
            super(itemView);
            result = itemView.findViewById(R.id.item_title);
            confidence = itemView.findViewById(R.id.item_confidence);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.mOnItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener{
        void onClick(int position);
    }
}






















