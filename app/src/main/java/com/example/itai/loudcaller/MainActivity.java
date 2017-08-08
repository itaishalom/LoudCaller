package com.example.itai.loudcaller;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.example.itai.loudcaller.GlobalFunctions.PREFS_NAME;
import static com.example.itai.loudcaller.GlobalFunctions.SETTINGS_IS_ON;
import static com.example.itai.loudcaller.GlobalFunctions.SETTINGS_NUMBERS;

public class MainActivity extends AppCompatActivity {
    String[] sArrFull;
    public ArrayList<String> allAddedPhoneNumbers;
    ListView lv1;
    Switch myOnOffSwitch;
    EditText inputSearch;
    ArrayAdapter<String> adapter;
    String selectContact;

    private String[] permissions = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_CONTACTS, Manifest.permission.RECEIVE_BOOT_COMPLETED, Manifest.permission.ACCESS_NOTIFICATION_POLICY};

    private static final int REQUESTS = 100;
    public final static int REQUEST_DENIED_PERMISSION = 1232;
    public final static int REQUEST_NOTIFICATION_CHANGE = 1231;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lv1 = (ListView) findViewById(R.id.listView1);
        myOnOffSwitch = (Switch) findViewById(R.id.onOff);
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        boolean isOn = settings.getBoolean(SETTINGS_IS_ON, true);
        myOnOffSwitch.setChecked(isOn);
        myOnOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    GlobalFunctions.saveToSettings(SETTINGS_IS_ON, true, MainActivity.this.getApplicationContext());
                } else {
                    GlobalFunctions.saveToSettings(SETTINGS_IS_ON, false, MainActivity.this.getApplicationContext());
                }
            }
        });
        ActivityCompat.requestPermissions(this, permissions, REQUESTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUESTS:
                boolean[] arrOfPermissions = new boolean[grantResults.length];
                for (int i = 0; i < arrOfPermissions.length; i++) {
                    arrOfPermissions[i] = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    String permission = permissions[i];
                    if (!arrOfPermissions[i]) {
                        boolean showRationale = false;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            showRationale = shouldShowRequestPermissionRationale(permission);
                        }
                        if (!showRationale) {
                            startSettings(REQUEST_DENIED_PERMISSION, Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            return;
                        } else {
                            popUpForRequest(null, 0);
                            return;
                        }
                    }
                }
                checkSoundPolicyPermission();
                break;
        }
    }

    public void checkSoundPolicyPermission() {
        /** check if we already  have permission to draw over other apps */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                /** if not construct intent to request permission */
                startSettings(REQUEST_NOTIFICATION_CHANGE, android.provider.Settings
                        .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            } else {
                readContacts();
            }
        } else {
            readContacts();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_NOTIFICATION_CHANGE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    NotificationManager notificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager.isNotificationPolicyAccessGranted()) {
                        readContacts();
                    } else {
                        startSettings(REQUEST_NOTIFICATION_CHANGE, android.provider.Settings
                                .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    }
                }
                break;
            case REQUEST_DENIED_PERMISSION:
                ActivityCompat.requestPermissions(this, permissions, REQUESTS);
                break;
        }
    }


    private void popUpForRequest(final Intent intent, final int code) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage(R.string.permission_requested_message)
                .setTitle(R.string.alert_dialog_Permission_Request);
        // Add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (intent != null)
                    startActivityForResult(intent, code);
                else
                    ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUESTS);
            }
        });
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void startSettings(final int code, String settings) {
        final Intent intent = new Intent(settings);
        popUpForRequest(intent, code);
    }

    private void readContacts() {
        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (phones == null) {
            return;
        }
        ArrayList<String> tempString = new ArrayList<>();
        while (phones.moveToNext()) {
            //Read Contact Name
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            //Read Phone Number
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            if (name != null) {
                String newMember = name + " : " + phoneNumber;
                String striped = PhoneNumberUtils.stripSeparators(phoneNumber);
                String newMember2 = name + " : " + striped;
                if (!tempString.contains(newMember) && !tempString.contains(newMember2)) {
                    tempString.add(newMember2);
                }
            }

        }
        phones.close();
        java.util.Collections.sort(tempString);
        fillContactsInList(tempString);
    }


    private void fillContactsInList(ArrayList<String> tempString) {
        sArrFull = tempString.toArray(new String[tempString.size()]);
        //Create Array Adapter and Pass ArrayOfValues to it.
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2, android.R.id.text1, sArrFull);
        java.util.Collections.sort(tempString);
        //BindAdpater with our Actual ListView
        lv1.setAdapter(adapter);
        inputSearch = (EditText) findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user changed the Text
                MainActivity.this.adapter.getFilter().filter(cs);
            }
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
            @Override
            public void afterTextChanged(Editable arg0) {}
        });
        lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Object o = lv1.getItemAtPosition(arg2);
                selectContact = o.toString();
                Toast.makeText(getBaseContext(), o.toString(), Toast.LENGTH_SHORT).show();
                String[] vals = selectContact.split(":");
                String name = vals[0].trim();
                String num = vals[1].trim();
                allAddedPhoneNumbers = GlobalFunctions.loadNumbers(MainActivity.this);
                AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                builder1.setMessage("Are you sure you wish to add " + name + "'s number " + num + " to your specials?");
                builder1.setCancelable(true);
                builder1.setPositiveButton(
                        "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String[] vals = selectContact.split(":");
                                String name = vals[0].trim();
                                String num = vals[1].trim();
                                if (allAddedPhoneNumbers.contains(num)) {
                                    Toast.makeText(getBaseContext(), "The number: " + num + " already exists", Toast.LENGTH_SHORT).show();
                                } else {
                                    allAddedPhoneNumbers.add(num);
                                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                                    Phonenumber.PhoneNumber phone = null;
                                    String numb = "";
                                    try {
                                        phone = PhoneNumberUtil.getInstance().parse(num, null);
                                    } catch (NumberParseException e) {
                                        e.printStackTrace();
                                        numb = num;
                                    }
                                    if (numb.isEmpty()) {
                                        numb = phoneUtil.format(phone, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                                        allAddedPhoneNumbers.add(PhoneNumberUtils.stripSeparators(numb));
                                    }
                                    Toast.makeText(getBaseContext(), "added: " + num, Toast.LENGTH_SHORT).show();
                                    Set tempSet = new HashSet(allAddedPhoneNumbers);
                                    GlobalFunctions.saveToSettings(SETTINGS_NUMBERS, tempSet, MainActivity.this);
                                }
                            }
                        });

                builder1.setNegativeButton(
                        "No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert11 = builder1.create();
                alert11.show();
            }
        });
    }
}
