package com.exteragram.messenger.preferences;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Stub preference screen pending full Extera port. */
public class ChatsPreferencesActivity extends BasePreferencesActivity {
    @Override
    protected String getTitle() {
        return "Chats";
    }

    @Override
    protected void updateRowsId() {
        rowCount = 0;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new BaseListAdapter(context) {
            @Override
            public int getItemCount() {
                return 0;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(new View(mContext)) {};
            }

            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public boolean isEnabled(RecyclerView.ViewHolder holder) {
                return false;
            }
        };
    }
}
