package org.davidadrian.andhoc;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import org.davidadrian.andhoc.service.AndHocMessage;
import org.davidadrian.andhoc.service.AndHocMessenger;
import org.davidadrian.andhoc.service.AndHocService;

import java.util.ArrayList;
import java.util.UUID;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private EditText mEditTextMessage;
    private Button mButtonSend;
    private Switch mSwitchBroadcast;
    private Switch mSwitchListen;
    private ListView mListViewMessages;

    private ArrayAdapter<String> mAdapter;

    private String mUser;

    private boolean mBound = false;
    private ServiceConnection mConnection;
    private AndHocService mService;
    private AndHocMessenger mMessenger;
    private AndHocService.AndHocMessageListener mListener;

    private ArrayList<String> mMessages = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditTextMessage = (EditText) findViewById(R.id.editTextMessage);
        mButtonSend = (Button) findViewById(R.id.buttonSend);
        mSwitchBroadcast = (Switch) findViewById(R.id.switchBroadcast);
        mSwitchListen = (Switch) findViewById(R.id.switchListen);
        mListViewMessages = (ListView) findViewById(R.id.listViewMessages);

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mMessages);
        mListViewMessages.setAdapter(mAdapter);

        mUser = UUID.randomUUID().toString();

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Connected to AndHoc service");
                AndHocService.AndHocBinder binder = (AndHocService.AndHocBinder) service;
                mService = binder.getAndHoc();
                mMessenger = mService.createUserMessenger(mUser);
                mBound = true;
                mSwitchBroadcast.setEnabled(true);
                mSwitchListen.setEnabled(true);
                mButtonSend.setEnabled(true);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Disconnected from AndHoc service");
                mBound = false;
                mService = null;
                mMessenger = null;
                mListener = null;
                mSwitchBroadcast.setEnabled(false);
                mSwitchListen.setEnabled(false);
                mButtonSend.setEnabled(false);
            }
        };
        Intent intent = new Intent(this, AndHocService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mBound) {
            stopBroadcast();
            removeListener();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClickSend(View view) {
        Log.d(TAG, "onClickSend called");
        // Should not happen
        if (!mBound) {
            Log.e(TAG, "Send clicked while not connected to service");
            Toast.makeText(this, "Error: Not connected to service", Toast.LENGTH_SHORT).show();
            return;
        }
        // Build the message
        String text = mEditTextMessage.getText().toString();
        AndHocMessage msg = mMessenger.createMessage(text);
        mMessenger.setMessage(msg);
        Toast.makeText(this, "Message set!", Toast.LENGTH_LONG).show();
    }

    public void onToggleBroadcast(View view) {
        if (!mBound) {
            mSwitchBroadcast.setChecked(false);
            mSwitchBroadcast.setEnabled(false);
            return;
        }
        boolean toggleOn = mSwitchBroadcast.isChecked();
        if (toggleOn) {
            startBroadcast();
        } else {
            stopBroadcast();
        }
    }

    public void onToggleListen(View view) {
        // Defensive programming, shouldn't happen
        if (!mBound) {
            mSwitchListen.setChecked(false);
            mSwitchListen.setEnabled(false);
            return;
        }
        // Respond to the button
        boolean toggleOn = mSwitchListen.isChecked();
        if (toggleOn) {
            addListener();
        } else {
            removeListener();
        }
    }

    private void startBroadcast() {
        mMessenger.broadcast();
    }

    private void stopBroadcast() {
        mMessenger.stopBroadcast();
    }

    private void removeListener() {
        if (mListener != null) {
            Log.d(TAG, "Removing listener");
            mService.removeListener(mListener);
        }
    }

    private void addListener() {
        Log.d(TAG, "Adding listener");
        mListener = new AndHocService.AndHocMessageListener() {
            @Override
            public void onNewMessage(AndHocMessage msg) {
                Log.d(TAG, "Adding message to list: " + msg.getMessage());
                mMessages.add(msg.getMessage());
                mAdapter.notifyDataSetChanged();
            }
        };
        mService.addMessageListener(mListener);
    }

}
