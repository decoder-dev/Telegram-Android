package org.telegram.banana;

import android.content.SharedPreferences;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.banana.BananaConfig;
import org.telegram.messenger.banana.BananaHooks;
import org.telegram.tgnet.TLRPC;
import static org.junit.Assert.*;

public class PollPreviewTest {
    @Test
    public void automaticResultsKeepVotingAndRespectHiddenResultsAndToggleChanges() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            SharedPreferences prefs = MessagesController.getMainSettings(0);
            String key = BananaConfig.Option.POLL_RESULTS_BEFORE_VOTING.key;
            Object old = prefs.getAll().get(key);
            try {
                TLRPC.TL_message raw = new TLRPC.TL_message();
                raw.id = 42; raw.message = "";
                raw.peer_id = new TLRPC.TL_peerUser(); raw.peer_id.user_id = 42;
                TLRPC.TL_messageMediaPoll media = new TLRPC.TL_messageMediaPoll();
                raw.media = media;
                media.poll = new TLRPC.TL_poll(); media.poll.id = 42;
                media.poll.question.text = "Choose";
                media.results = new TLRPC.TL_pollResults();
                MessageObject message = new MessageObject(0, raw, false, false);
                BananaConfig.set(0, BananaConfig.Option.POLL_RESULTS_BEFORE_VOTING, true);
                assertTrue(BananaHooks.previewPollResults(0, message));
                assertFalse("Auto results must not set the manual mode that disables voting", message.forceShowPollResults);
                media.poll.hide_results_until_close = true;
                assertFalse(BananaHooks.previewPollResults(0, message));
                media.poll.hide_results_until_close = false;
                BananaConfig.set(0, BananaConfig.Option.POLL_RESULTS_BEFORE_VOTING, false);
                assertFalse("The same message must stop auto preview when disabled", BananaHooks.previewPollResults(0, message));
                message.forceShowPollResults = true;
                assertTrue("Explicit author preview remains independent", BananaHooks.previewPollResults(0, message));
            } finally {
                SharedPreferences.Editor restore = prefs.edit().remove(key);
                if (old instanceof Boolean) restore.putBoolean(key, (Boolean) old);
                restore.apply();
            }
        });
    }
}
