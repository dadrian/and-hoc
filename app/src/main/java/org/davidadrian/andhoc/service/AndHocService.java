package org.davidadrian.andhoc.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.davidadrian.andhoc.service.impl.BasicAndHocMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AndHocService extends Service {

    private static final String TAG = "AndHocService";

    private boolean mFirstBind = true;

    private AndHocBinder mBinder;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mReceiver;

    private WifiP2pDnsSdServiceInfo mServiceInfo;

    private ArrayList<AndHocMessageListener> mListeners = new ArrayList<AndHocMessageListener>();

    public static interface AndHocMessageListener {

        public void onNewMessage(AndHocMessage msg);

    };

    public class AndHocBinder extends Binder {

        public AndHocService getAndHoc() {
            return AndHocService.this;
        }
    };

    public class SingleUserMessenger implements AndHocMessenger {

        private String mUser;
        private AndHocMessage mMessage;
        private boolean mBroadcasting = false;

        protected SingleUserMessenger(String username) {
            mUser = username;
        }

        @Override
        public AndHocMessage createMessage(String messageText) {
            return new BasicAndHocMessage(mUser, messageText);
        }

        @Override
        public void setMessage(AndHocMessage message) {
            mMessage = message;
            if (mBroadcasting) {
                broadcast();
            }
        }

        @Override
        public void broadcast() {
            if (mMessage == null) {
                Log.d(TAG, "Not broadcasting (message is null)");
                return;
            }
            AndHocService.this.setBroadcast(mMessage);
            mBroadcasting = true;
        }

        @Override
        public void stopBroadcast() {
            mBroadcasting = false;
            AndHocService.this.removeBroadcast();
        }
    }

    public AndHocService() {
        super();
        // Set up the service info
    }


    @Override
    public IBinder onBind(Intent intent) {
        if (mFirstBind) {
            mBinder = new AndHocBinder();
            mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            mChannel = mManager.initialize(this, getMainLooper(), null);

            // Set up broadcast receiver
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
            mReceiver = new BroadcastReceiver() {

                private static final String TAG = "AndHocService.BroadcastReceiver";

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "onReceive called");

                    String action = intent.getAction();

                    if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                        // Check to see if Wi-Fi is enabled and notify appropriate activity
                        Log.d(TAG, "Wifi P2p State Changed");
                    } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                        // Call WifiP2pManager.requestPeers() to get a list of current peers
                        Log.d(TAG, "Wifi P2p Peers Changed");
                    } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                        // Respond to new connection or disconnections
                        Log.d(TAG, "Wifi P2p Connections changed");
                    } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                        // Respond to this device's wifi state changing
                        Log.d(TAG, "Wifi P2p This Device Changed");
                    }
                }
            };
            registerReceiver(mReceiver, mIntentFilter);
            listen();
        }
        return mBinder;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
    }

    /* Client Methods */
    public void addMessageListener(AndHocMessageListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(AndHocMessageListener listener) {
        mListeners.remove(listener);
    }

    public AndHocMessenger createUserMessenger(String user) {
        return new SingleUserMessenger(user);
    }

    /* Private internal methods */
    private static AndHocMessage fromTxtMap(Map<String, String> record) {
        if (!(record.containsKey("name") && record.containsKey("msg"))) {
            return null;
        }
        return new BasicAndHocMessage(record.get("user"), record.get("msg"));
    }


    private void listen() {
        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName,
                                                  Map<String, String> txtRecordMap,
                                                  WifiP2pDevice srcDevice) {
                Log.d(TAG, "Record available: " + txtRecordMap.toString());
                AndHocMessage msg = fromTxtMap(txtRecordMap);
                if (msg != null) {
                    for (AndHocMessageListener listener : mListeners) {
                        listener.onNewMessage(msg);
                    }
                }
            }
        };
        WifiP2pManager.DnsSdServiceResponseListener servListener =
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                        WifiP2pDevice resourceType) {
                        // Update the device name with the human-friendly version from
                        // the DnsTxtRecord, assuming one arrived.
                        Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
                    }

                };
        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
        WifiP2pServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Added service request");
                mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Discovering services!");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.e(TAG, "Failed to call discover services");
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to add service request due to error " + reason);
            }
        });
    }

    private void removeBroadcast() {
        if (mServiceInfo == null) {
            return;
        }
        mManager.removeLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "removeBroadcast callback success!");
                mServiceInfo = null;
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Could not remove broadcast");
            }
        });
    }

    private void setBroadcast(AndHocMessage msg) {
        final Map<String, String> record = new HashMap();
        record.put("name", msg.getName());
        record.put("msg", msg.getMessage());

        if (mServiceInfo != null) {
            mManager.removeLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Removed old broadcast");
                    mServiceInfo = null;
                    finishSetBroadcast(record);
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Failed to remove old broadcast");
                }
            });
        } else {
            finishSetBroadcast(record);
        }
    }

    private void finishSetBroadcast(Map<String, String> record) {
        mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("_test",
                "_presence._tcp", record);
        mManager.addLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Added DNS service");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failed to add DNS service");
            }
        });
    }



}
