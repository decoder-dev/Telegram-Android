/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCheckCell2;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SlideChooseView;

public abstract class BasePreferencesActivity extends BaseFragment {

    protected static final Object payload = new Object();

    protected RecyclerListView listView;
    protected BaseListAdapter listAdapter;
    protected LinearLayoutManager layoutManager;
    protected Theme.ResourcesProvider resourcesProvider;

    protected int rowCount;


    protected int newRow(int rows) {
        return rowCount += rows;
    }

    protected int newRow() {
        return rowCount++;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRowsId();
        return true;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    protected void updateRowsId() {
        rowCount = 0;
    }

    // Telegram's simple glass layout: centred title on a glass header, grouped rounded sections.
    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return useSimpleActionBar();
    }

    @Override
    public View createView(Context context) {
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        ActionBar.ActionBarMenuOnItemClick onItemClick = new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        };
        if (simpleActionBar != null) {
            setHasOwnBackground(true);
            simpleActionBar.setBackButtonDrawable(new BackDrawable(false));
            simpleActionBar.setTitle(getTitle());
            simpleActionBar.setActionBarMenuOnItemClick(onItemClick);
        } else {
            fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setTitle(getTitle());
            actionBar.setOccupyStatusBar(!AndroidUtilities.isTablet());
            actionBar.setActionBarMenuOnItemClick(onItemClick);
            actionBar.setAllowOverlayTitle(false);
        }

        listView = new RecyclerListView(context);
        if (simpleActionBar != null) {
            listView.setSections();
            listView.setSectionsDrawBackground(true);
        }
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);

        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setChangeDuration(350);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        listView.setItemAnimator(itemAnimator);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setAdapter(listAdapter = createAdapter(context));
        listView.setOnItemClickListener(this::onItemClick);

        return fragmentView;
    }

    protected void showBulletin() {
        BulletinFactory.of(this).createErrorBulletin(LocaleController.getString("RestartRequired", R.string.RestartRequired), resourcesProvider).show();
    }

    protected RecyclerListView getListView() {
        return listView;
    }

    protected boolean hasWhiteActionBar() {
        return false;
    }

    @Override
    public boolean isLightStatusBar() {
        if (!hasWhiteActionBar()) return super.isLightStatusBar();
        int color = getThemedColor(Theme.key_windowBackgroundWhite);
        return ColorUtils.calculateLuminance(color) > 0.7f;
    }

    protected abstract String getTitle();

    protected abstract BaseListAdapter createAdapter(Context context);

    protected abstract void onItemClick(View view, int position, float x, float y);

    protected abstract class BaseListAdapter extends RecyclerListView.SelectionAdapter {

        protected final Context mContext;

        public BaseListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 2 || type == 5 || type == 7 || type == 16 || type == 18 || type == 19;
        }

        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            onBindViewHolder(holder, position, false);
            // Screens bind the old grey divider shadow; sections draw their own gaps.
            if (listView != null && listView.hasSections() && holder.itemView instanceof ShadowSectionCell) {
                holder.itemView.setBackground(null);
            }
        }

        /** Cards are drawn by the sections; only the legacy flat layout needs a white row. */
        protected void setRowBackground(View view) {
            if (listView == null || !listView.hasSections()) {
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 1:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 2:
                    view = new TextCell(mContext);
                    setRowBackground(view);
                    break;
                case 3:
                    view = new HeaderCell(mContext);
                    setRowBackground(view);
                    break;
                case 4:
                    view = new HeaderCell(mContext);
                    setRowBackground(view);
                    break;
                case 5:
                    view = new TextCheckCell(mContext);
                    setRowBackground(view);
                    break;
                case 7:
                    view = new TextSettingsCell(mContext);
                    setRowBackground(view);
                    break;
                case 8:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                // case 9: Appearance > AvatarCornersPreviewCell
                // case 10: Chats > StickerShapeCell
                // case 11: Chats > StickerSizeCell
                // case 12: Appearance > FabShapeCell
                case 13:
                    view = new SlideChooseView(mContext);
                    setRowBackground(view);
                    break;
                // case 14: General > ActionBarSetupCell, Appearance > FoldersPreviewCell
                // case 15: Chats > DoubleTapCell, Appearance > SolarIconsPreviewCell
                // case 16: Chats > SetReactionCell
                // case 17: General > CameraTypeSelector, Appearance > ChatListPreviewCell
                case 18:
                    view = new TextCheckCell2(mContext);
                    setRowBackground(view);
                    break;
                case 19:
                    CheckBoxCell checkBoxCell = new CheckBoxCell(mContext, 4, 21, getResourceProvider());
                    checkBoxCell.getCheckBoxRound().setDrawBackgroundAsArc(14);
                    checkBoxCell.getCheckBoxRound().setColor(Theme.key_switch2TrackChecked, Theme.key_radioBackground, Theme.key_checkboxCheck);
                    checkBoxCell.setEnabled(true);
                    view = checkBoxCell;
                    setRowBackground(view);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + viewType);
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }
    }
}