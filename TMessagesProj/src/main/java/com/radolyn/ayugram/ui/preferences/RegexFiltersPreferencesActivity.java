/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.ui.preferences;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.exteragram.messenger.preferences.BasePreferencesActivity;
import com.radolyn.ayugram.AyuConfig;
import com.radolyn.ayugram.ui.preferences.utils.RegexFilterPopup;
import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

public class RegexFiltersPreferencesActivity extends BasePreferencesActivity {

    private int filtersHeaderRow;

    // .. filters

    private int filtersDividerRow;
    private int addFilterBtnRow;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        filtersHeaderRow = newRow();

        var filters = AyuConfig.getRegexFilters();
        rowCount += filters.size();

        filtersDividerRow = newRow();
        addFilterBtnRow = newRow();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
       if (position > filtersHeaderRow && position < filtersDividerRow) {
           // clicked on filter
           RegexFilterPopup.show(this, view, x, y, position - filtersHeaderRow - 1);
       } else if (position == addFilterBtnRow) {
            presentFragment(new RegexFilterEditActivity(-1));
       }
    }

    @Override
    public void onResume() {
        updateRowsId();

        super.onResume();
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString(R.string.RegexFilters);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 1:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 2:
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position > filtersHeaderRow && position < filtersDividerRow) {
                        textCell.setText(AyuConfig.getRegexFilters().get(position - filtersHeaderRow - 1), true);
                    } else if (position == addFilterBtnRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.RegexFiltersAdd), R.drawable.msg_add, false);
                    }
                    break;
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == filtersHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.RegexFiltersHeader));
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (
                    position == filtersDividerRow
            ) {
                return 1;
            } else if (
                    position == filtersHeaderRow
            ) {
                return 3;
            }
            return 2;
        }
    }
}
