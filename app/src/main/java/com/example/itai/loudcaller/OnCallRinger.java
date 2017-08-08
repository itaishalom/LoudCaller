package com.example.itai.loudcaller;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;

import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;
import static android.media.AudioManager.RINGER_MODE_NORMAL;
import static com.example.itai.loudcaller.GlobalFunctions.PREFS_NAME;
import static com.example.itai.loudcaller.GlobalFunctions.SETTINGS_IS_ON;

/**
 * Proudly written by Itai on 05/08/2017.
 */

public class OnCallRinger extends IncomingCallReceiver {
    static boolean bIncomingCall = false;
    static int CurrentRingerMode = -1;
    static int currentInterruptionFilter = -1;
    private static boolean isAnswered;

    protected void onIncomingCallReceived(Context ctx, String number) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        CurrentRingerMode = am.getRingerMode();
        String stripedNumber = PhoneNumberUtils.stripSeparators(number);
        String secondNumber = "";
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber phone = null;
        try {
            phone = PhoneNumberUtil.getInstance().parse(number, null);
        } catch (NumberParseException e) {
            e.printStackTrace();
            secondNumber = number;
        }
        if (secondNumber.isEmpty()) {
            secondNumber = phoneUtil.format(phone, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            secondNumber = PhoneNumberUtils.stripSeparators(secondNumber);
        }
        if (!shouldProceed(ctx, number, secondNumber, stripedNumber)) {
            return;
        }
        isAnswered = false;
        SharedPreferences settings = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        boolean isOn = settings.getBoolean(SETTINGS_IS_ON, true);
        ArrayList<String> allAddedPhoneNumbers = GlobalFunctions.loadNumbers(ctx.getApplicationContext());
        if ((allAddedPhoneNumbers.contains(secondNumber) || allAddedPhoneNumbers.contains(number)) && isOn) {
            am.setRingerMode(RINGER_MODE_NORMAL);
            try {//
                final MediaPlayer mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDataSource(ctx.getApplicationContext(), RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_RINGTONE));
                mMediaPlayer.setVolume(1.0f, 1.0f);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
                bIncomingCall = true;
                new CountDownTimer(30000, 100) {

                    public void onTick(long millisUntilFinished) {
                        if (isAnswered) {
                            mMediaPlayer.stop();
                            this.cancel();
                        }
                    }

                    public void onFinish() {
                        mMediaPlayer.stop();
                    }
                }.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, String number) {
        isAnswered = true;
        returnToSilent(ctx);
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number) {
        isAnswered = true;
        //if was on silent mode, and rang, now should return to silent mode
        returnToSilent(ctx);
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number) {
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number) {
    }

    @Override
    protected void onMissedCall(Context ctx, String number) {
        isAnswered = true;
        returnToSilent(ctx);
    }

    private void returnToSilent(Context ctx) {
        SharedPreferences settings = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        boolean isOn = settings.getBoolean(SETTINGS_IS_ON, true);
        //if was on silent mode, and rang, now should return to silent mode
        if (isOn && bIncomingCall) {
            AudioManager am;
            am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (CurrentRingerMode > -1) {
                am.setRingerMode(CurrentRingerMode);
                CurrentRingerMode= -1;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (currentInterruptionFilter > -1) {
                        NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.setInterruptionFilter(currentInterruptionFilter);
                        currentInterruptionFilter = -1;
                    }
                }
            }
            bIncomingCall = false;
        }
    }

    private boolean shouldProceed(Context ctx, String number, String secondNumber, String stripedNumber) {
        if (RINGER_MODE_NORMAL == CurrentRingerMode) {
            return false; //It rings
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            currentInterruptionFilter = mNotificationManager.getCurrentInterruptionFilter();
            if (currentInterruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
                Cursor phones = ctx.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        "starred=? AND (" + ContactsContract.CommonDataKinds.Phone.NUMBER + "=? OR " +
                                ContactsContract.CommonDataKinds.Phone.NUMBER + "=? OR " +
                                ContactsContract.CommonDataKinds.Phone.NUMBER + "=?)",
                        new String[]{"1", number, secondNumber, stripedNumber}, null);
                if (phones != null && phones.getCount() > 0) {
                    return false; //It will ring without us
                }
            }
        }
        return true;
    }
}
