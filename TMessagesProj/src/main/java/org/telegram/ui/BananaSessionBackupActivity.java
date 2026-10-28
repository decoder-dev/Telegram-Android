package org.telegram.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;
import android.text.InputType;
import org.telegram.messenger.*;
import org.telegram.messenger.banana.*;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Cells.BananaSwitchCell;
import org.telegram.ui.Components.*;
import java.util.ArrayList;

/** Explicit password export/import; automatic copies stay in app-private encrypted storage. */
public final class BananaSessionBackupActivity extends BananaSettingsFragment {
    private static final int EXPORT = 810, IMPORT = 811;
    public BananaSessionBackupActivity(int account) { setCurrentAccount(account); }
    @Override public Theme.ResourcesProvider getResourceProvider() { return BananaTheme.forExtras(); }
    @Override protected CharSequence getTitle() { return LocaleController.getString(R.string.BananaSessionBackup); }
    @Override protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        boolean active = UserConfig.getInstance(currentAccount).isClientActivated();
        items.add(BananaSwitchCell.Factory.of(1, LocaleController.getString(R.string.BananaSessionAutomatic), BananaConfig.get(currentAccount, BananaConfig.Option.SESSION_KEYCHAIN_BACKUP)).setEnabled(active));
        items.add(UItem.asButton(EXPORT, LocaleController.getString(R.string.BananaSessionExport)).setEnabled(active));
        items.add(UItem.asButton(IMPORT, LocaleController.getString(R.string.BananaSessionImport)));
        for (int slot = 0; slot < UserConfig.MAX_ACCOUNT_COUNT; slot++) {
            final int source = slot;
            java.io.File local = new java.io.File(ApplicationLoader.applicationContext.getNoBackupFilesDir(), "banana-sessions/slot-" + source + ".bin");
            if (local.isFile()) items.add(UItem.asButton(900 + slot, LocaleController.formatString(R.string.BananaSessionLocal, slot + 1)));
        }
        items.add(UItem.asShadow(LocaleController.getString(R.string.BananaSessionInfo)));
    }
    @Override protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 1) {
            boolean next = !BananaConfig.get(currentAccount, BananaConfig.Option.SESSION_KEYCHAIN_BACKUP);
            BananaConfig.set(currentAccount, BananaConfig.Option.SESSION_KEYCHAIN_BACKUP, next);
            if (next) BananaSessionBackup.schedule(currentAccount); else BananaSessionBackup.clear(currentAccount);
            listView.adapter.update(true); return;
        }
        if (item.id == EXPORT || item.id == IMPORT) {
            Intent intent = new Intent(item.id == EXPORT ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE).setType("application/octet-stream");
            if (item.id == EXPORT) intent.putExtra(Intent.EXTRA_TITLE, "BananaGram-session.bns");
            startActivityForResult(intent, item.id); return;
        }
        if (item.id >= 900 && item.id < 900 + UserConfig.MAX_ACCOUNT_COUNT) {
            progress(done -> BananaSessionBackup.restoreLocal(item.id - 900, done), true);
        }
    }
    @Override protected boolean onLongClick(UItem item, View view, int position, float x, float y) { return false; }
    @Override public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if ((requestCode != EXPORT && requestCode != IMPORT) || resultCode != Activity.RESULT_OK || data == null || data.getData() == null || getParentActivity() == null) return;
        Uri uri = data.getData();
        EditText field = new EditText(getParentActivity());
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setHint(LocaleController.getString(R.string.BananaSessionPassword));
        AlertDialog dialog = new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(LocaleController.getString(R.string.BananaSessionPassword))
                .setMessage(LocaleController.getString(R.string.BananaSessionPasswordInfo)).setView(field)
                .setPositiveButton(LocaleController.getString(R.string.OK), (d, w) -> {
                    char[] password = field.getText().toString().toCharArray(); field.setText("");
                    if (password.length < 10) { java.util.Arrays.fill(password, '\0'); message(R.string.BananaSessionPasswordInfo); return; }
                    if (requestCode == EXPORT) progress(done -> BananaSessionBackup.export(currentAccount, uri, password, done), false);
                    else progress(done -> BananaSessionBackup.importBackup(uri, password, done), true);
                }).setNegativeButton(LocaleController.getString(R.string.Cancel), null).create();
        showDialog(dialog);
    }
    private interface Operation { void run(java.util.function.Consumer<Boolean> done); }
    private void progress(Operation action, boolean restore) {
        AlertDialog progress = new AlertDialog(getParentActivity(), 3); progress.setCancelable(false); progress.show();
        action.run(ok -> {
            try { progress.dismiss(); } catch (Exception ignored) { }
            if (getParentActivity() == null) { if (restore && ok) BananaSessionBackup.cancelPendingRestore(); return; }
            if (!ok) { message(R.string.BananaSessionFailed); return; }
            if (!restore) { message(R.string.BananaSessionSaved); return; }
            AlertDialog restartDialog = new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                    .setTitle(LocaleController.getString(R.string.BananaSessionRestart))
                    .setMessage(LocaleController.getString(R.string.BananaSessionRestartInfo))
                    .setPositiveButton(LocaleController.getString(R.string.BananaSessionRestart), (d, w) -> {
                        BananaSessionBackup.confirmPendingRestore(ready -> {
                            if (!ready) { message(R.string.BananaSessionFailed); return; }
                        android.content.Context context = ApplicationLoader.applicationContext;
                        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        android.app.PendingIntent restart = android.app.PendingIntent.getActivity(context, 9817, launch,
                                android.app.PendingIntent.FLAG_CANCEL_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
                        android.app.AlarmManager alarms = (android.app.AlarmManager) context.getSystemService(android.content.Context.ALARM_SERVICE);
                        alarms.set(android.app.AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + 1000, restart);
                        android.os.Process.killProcess(android.os.Process.myPid());
                        });
                    }).setNegativeButton(LocaleController.getString(R.string.Cancel), (d, w) -> BananaSessionBackup.cancelPendingRestore()).create();
            restartDialog.setCancelable(false);
            restartDialog.setCanceledOnTouchOutside(false);
            showDialog(restartDialog);
        });
    }
    private void message(int resource) { if (getParentActivity() == null) return; showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider()).setMessage(LocaleController.getString(resource)).setPositiveButton(LocaleController.getString(R.string.OK), null).create()); }
}
