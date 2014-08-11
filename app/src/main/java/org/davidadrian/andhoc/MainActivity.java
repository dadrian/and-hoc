package org.davidadrian.andhoc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceInfo;
import android.os.AsyncTask;
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
import android.widget.Toast;

import org.davidadrian.andhoc.service.AndHocMessage;
import org.davidadrian.andhoc.service.AndHocMessenger;
import org.davidadrian.andhoc.service.AndHocService;
import org.davidadrian.andhoc.service.impl.BasicAndHocMessage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private EditText mEditTextMessage;
    private Button mButtonBroadcast;
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
        mButtonBroadcast = (Button) findViewById(R.id.buttonBroadcast);
        mListViewMessages = (ListView) findViewById(R.id.listViewMessages);

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mMessages);
        mListViewMessages.setAdapter(mAdapter);

        mUser = "User " + (int) Math.random() % 1000;

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Connected to AndHoc service");
                AndHocService.AndHocBinder binder = (AndHocService.AndHocBinder) service;
                mService = binder.getAndHoc();
                mMessenger = mService.createUserMessenger(mUser);
                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Disconnected from AndHoc service");
                mBound = false;
                mService = null;
                mMessenger = null;
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
        if (mBound) {
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

    @Override
    public void onPause() {
        super.onPause();
        if (mBound && mListener != null) {
            Log.d(TAG, "Removing listener");
            mService.removeListener(mListener);
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

    public void onClickBroadcast(View view) {
        Log.d(TAG, "onClickBroadcast called");
        if (!mBound) {
            Toast.makeText(this, "Error: Not connected to service", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = mEditTextMessage.getText().toString();
        AndHocMessage msg = mMessenger.createMessage(text);
        mMessenger.send(msg);
        Toast.makeText(this, "Broadcasting message!", Toast.LENGTH_LONG).show();
    }

}
