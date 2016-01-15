/*
 * Copyright 2016 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;

import com.andrada.sitracker.contracts.AppUriContract;

/**
 * Android Beam helper methods.
 */
public class BeamUtils {

    /**
     * Sets this activity's Android Beam message to one representing the given author.
     */
    public static void setBeamAuthorUri(Activity activity, Uri authorUri) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter == null) {
            // No NFC :-(
            return;
        }

        nfcAdapter.setNdefPushMessage(new NdefMessage(
                new NdefRecord[]{
                        new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                                AppUriContract.Authors.CONTENT_ITEM_TYPE.getBytes(),
                                new byte[0],
                                authorUri.toString().getBytes())
                }), activity);
    }

    /**
     * Sets this activity's Android Beam message to one representing the given publication.
     */
    public static void setBeamPublicationUri(Activity activity, Uri publicaitonUri) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter == null) {
            // No NFC :-(
            return;
        }

        nfcAdapter.setNdefPushMessage(new NdefMessage(
                new NdefRecord[]{
                        new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                                AppUriContract.Authors.CONTENT_ITEM_TYPE.getBytes(),
                                new byte[0],
                                publicaitonUri.toString().getBytes())
                }), activity);
    }

    /**
     * Checks to see if the activity's intent ({@link android.app.Activity#getIntent()}) is
     * an NFC intent that the app recognizes. If it is, then parse the NFC message and set the
     * activity's intent (using {@link Activity#setIntent(android.content.Intent)}) to something
     * the app can recognize (i.e. a normal {@link android.content.Intent#ACTION_VIEW} intent).
     */
    public static void tryUpdateIntentFromBeam(Activity activity) {
        Intent originalIntent = activity.getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(originalIntent.getAction())) {
            Parcelable[] rawMsgs = originalIntent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            // Record 0 contains the MIME type, record 1 is the AAR, if present.
            // In sitracker, AARs are not present.
            NdefRecord mimeRecord = msg.getRecords()[0];
            if (AppUriContract.Authors.CONTENT_ITEM_TYPE.equals(
                    new String(mimeRecord.getType()))) {
                // Re-set the activity's intent to one that represents an author urlId.
                Intent authorUrlIdIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(new String(mimeRecord.getPayload())));
                activity.setIntent(authorUrlIdIntent);
            } else if (AppUriContract.Publications.CONTENT_ITEM_TYPE.equals(
                    new String(mimeRecord.getType()))) {
                // Re-set the activity's intent to one that represents an author urlId.
                Intent authorUrlIdIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(new String(mimeRecord.getPayload())));
                activity.setIntent(authorUrlIdIntent);
            }
        }
    }
}
