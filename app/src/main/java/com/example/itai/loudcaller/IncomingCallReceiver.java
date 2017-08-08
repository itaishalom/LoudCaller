package com.example.itai.loudcaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.Toast;

/**
 * Proudly written by Itai on 05/08/2017.
 */

public abstract class IncomingCallReceiver extends BroadcastReceiver {

    //The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations

    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static boolean isIncoming;
    private static String savedNumber;  //because the passed incoming is only valid in ringing


    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED") || intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON") ){
                Toast.makeText(context,"Loud caller starting", Toast.LENGTH_SHORT).show();
                return;
            }
            //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
            if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
                savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
            } else {
                String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
                String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                if (number != null) {
                    number = number.replaceAll("[^0-9.]", "");
                }else{
                    return;
                }
                int state = 0;

                if (stateStr != null) {
                    if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        state = TelephonyManager.CALL_STATE_IDLE;
                    } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                        state = TelephonyManager.CALL_STATE_OFFHOOK;
                    } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        state = TelephonyManager.CALL_STATE_RINGING;
                    }
                }
                onCallStateChanged(context, state, number);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //Derived classes should override these to respond to specific events of interest
    protected abstract void onIncomingCallReceived(Context ctx, String number);

    protected abstract void onIncomingCallAnswered(Context ctx, String number);

    protected abstract void onIncomingCallEnded(Context ctx, String number);

    protected abstract void onOutgoingCallStarted(Context ctx, String number);

    protected abstract void onOutgoingCallEnded(Context ctx, String number);

    protected abstract void onMissedCall(Context ctx, String number);

    //Deals with actual events
    public void onCallStateChanged(Context context, int state, String number) {
        if (lastState == state) {
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                savedNumber = number;
                //   Context context2 = getApplicationContext();
                onIncomingCallReceived(context, number);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    onOutgoingCallStarted(context, savedNumber);
                } else {
                    isIncoming = true;
                    onIncomingCallAnswered(context, savedNumber);
                }

                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                    onMissedCall(context, savedNumber);
                } else if (isIncoming) {
                    onIncomingCallEnded(context, savedNumber);
                } else {
                    onOutgoingCallEnded(context, savedNumber);
                }
                break;
        }
        lastState = state;
    }

}
