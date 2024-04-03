package com.ontrac.warehouse;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class ButtonAdapter extends RecyclerView.Adapter<ButtonAdapter.ViewHolder> {

//    private String[] localDataSet;

    private class ButtonModel {
        String label;
        String description;
        String id;
    }

//    List<ButtonModel> items;
    ButtonModel[] localDataSet;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final Button button;

        public ViewHolder(View view) {
            super(view);

            button = (Button) view.findViewById(R.id.button);
        }

        public Button getButton() {
            return button;
        }
    }

    public ButtonAdapter(ButtonModel[] dataSet) {
        localDataSet = dataSet;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.button_row_item, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
//        viewHolder.getButton().setText(localDataSet[position].label);
        viewHolder.getButton().setText(localDataSet[position].id);
    }

    @Override
    public int getItemCount() {
        return localDataSet.length;
    }
}
