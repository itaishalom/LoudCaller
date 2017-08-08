package com.example.itai.loudcaller;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.widget.Toast;

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
    static int CurrnetRingerMode = -1;
    private static boolean isAnswered;

    protected void onIncomingCallReceived(Context ctx, String number) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        CurrnetRingerMode = am.getRingerMode();
        if(!shouldProceed(ctx,number)){
            return;
        }
        number = PhoneNumberUtils.stripSeparators(number);
        String secondNumber = "";
        String name = "";
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber phone = null;
        try {
            phone = PhoneNumberUtil.getInstance().parse(number, null);
        } catch (NumberParseException e) {
            e.printStackTrace();
            secondNumber = number;
        }
        if(secondNumber.isEmpty()) {
            secondNumber = phoneUtil.format(phone, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            secondNumber = PhoneNumberUtils.stripSeparators(secondNumber);
        }
     /*   Cursor phones = ctx.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.NUMBER+"=?",
                new String[]{number,secondNumber}, null);
        while (phones != null && phones.moveToNext()) {
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
             name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        }*/

        isAnswered = false;
        SharedPreferences settings = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        boolean isOn = settings.getBoolean(SETTINGS_IS_ON, true);
        ArrayList<String> allAddedPhoneNumbers = GlobalFunctions.loadNumbers(ctx.getApplicationContext());
        if ( (allAddedPhoneNumbers.contains(secondNumber) || allAddedPhoneNumbers.contains(number) ) && isOn) {
            am.setRingerMode(RINGER_MODE_NORMAL);
            try {//
                final Ringtone r = RingtoneManager.getRingtone(ctx, RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_RINGTONE));
                r.play();
                bIncomingCall = true;
                new CountDownTimer(30000, 100) {

                    public void onTick(long millisUntilFinished) {
                        if (isAnswered) {
                            r.stop();
                            this.cancel();
                        }
                    }

                    public void onFinish() {
                        r.stop();
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
        returnToSilent(ctx, number);
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number) {
        isAnswered = true;
        //if was on silent mode, and rang, now should return to silent mode
        returnToSilent(ctx, number);
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number) {
        returnToSilent(ctx, number);
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number) {
        returnToSilent(ctx, number);
    }

    @Override
    protected void onMissedCall(Context ctx, String number) {
        isAnswered = true;
        returnToSilent(ctx, number);
    }

    private void returnToSilent(Context ctx, String number) {
        SharedPreferences settings = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        boolean isOn = settings.getBoolean(SETTINGS_IS_ON, true);
        ArrayList<String> allAddedPhoneNumbers = GlobalFunctions.loadNumbers(ctx.getApplicationContext());
        //if was on silent mode, and rang, now should return to silent mode
        if (allAddedPhoneNumbers.contains(number) && isOn && bIncomingCall) {
            AudioManager am;
            am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (CurrnetRingerMode > -1)
                am.setRingerMode(CurrnetRingerMode);
            bIncomingCall = false;
        }
    }

    private boolean shouldProceed(Context ctx, String number){
        if (RINGER_MODE_NORMAL == CurrnetRingerMode) {
            return false; //It rings
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            int num = mNotificationManager.getCurrentInterruptionFilter();
            if (num == INTERRUPTION_FILTER_PRIORITY) {
                Cursor phones = ctx.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, "starred=?",
                        new String[]{"1"}, null);
                while (phones != null && phones.moveToNext()) {
                    String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String digits = phoneNumber.replaceAll("[^0-9.]", "");
                    if (digits.equals(number)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
