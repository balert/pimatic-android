package org.pimatic.connection;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pimatic.model.ConnectionOptions;
import org.pimatic.model.DeviceManager;
import org.pimatic.model.DevicePageManager;
import org.pimatic.model.GroupManager;
import org.pimatic.model.PermissionManager;

import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * Created by Oliver Schneider <oliverschneider89+sweetpi@gmail.com>
 */
public class SocketClient {

    Socket socket;
    Activity mainActivity;
    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mainActivity.getApplicationContext(),
                            "Connection Errror", Toast.LENGTH_LONG).show();
                }
            });
        }
    };


    public SocketClient(final Activity mainActivity, final ConnectionOptions conOpts) {
        this.mainActivity = mainActivity;
        IO.Options opts = new IO.Options();
        opts.query = "username=" + conOpts.username + "&password=" + conOpts.password;
        opts.reconnection = true;
        opts.forceNew = true;
        final DeviceManager deviceManager = DeviceManager.getInstance();
        final GroupManager groupManager = GroupManager.getInstance();
        final DevicePageManager devicePageManager = DevicePageManager.getInstance();
        try {
            socket = IO.socket(conOpts.getBaseUrl(), opts);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.v("connect", Arrays.toString(args));
                }

            }).on("hello", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject helloObject = (JSONObject) args[0];
                    Log.v("hello", Arrays.toString(args));
                    try {
                        final JSONObject permissionsObject = helloObject.getJSONObject("permissions");

                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            try {
                                PermissionManager.updateFromJson(permissionsObject);
                                JSONObject params = new JSONObject();
                                params.put("criteria", new JSONObject(){
                                    {
                                        put("level","error");
                                    }
                                });
                                SocketClient.this.call("queryMessagesCount", params);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            }
                        });
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).on("devices", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONArray deviceArray = (JSONArray) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                deviceManager.updateFromJson(deviceArray);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
            }).on("deviceChanged", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject deviceObject = (JSONObject) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                deviceManager.updateDeviceFromJson(deviceObject);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
            }).on("deviceAdded", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject deviceObject = (JSONObject) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                deviceManager.addDeviceFromJson(deviceObject);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
            }).on("deviceRemoved", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject deviceObject = (JSONObject) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                deviceManager.removeDeviceById(deviceObject.getString("id"));
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }

            }).on("deviceAttributeChanged", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject eventObject = (JSONObject) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                deviceManager.deviceAttributeChanged(
                                        eventObject.getString("deviceId"),
                                        eventObject.getString("attributeName"),
                                        eventObject.getInt("time"),
                                        eventObject.get("value")
                                );
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });

                }
            }).on("pages", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONArray devicePageArray = (JSONArray) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                devicePageManager.updateFromJson(devicePageArray);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
            }).on("pageChanged", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject page = (JSONObject) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                devicePageManager.updateDevicePageFromJson(page);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
            }).on("pageAdded", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject page = (JSONObject) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                devicePageManager.addDevicePageFromJson(page);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
            }).on("pageRemoved", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject devicePageObject = (JSONObject) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                devicePageManager.removeDevicePageById(devicePageObject.getString("id"));
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }

            }).on("groups", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONArray groupsArray = (JSONArray) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                groupManager.updateFromJson(groupsArray);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
            }).on("groupChanged", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject group = (JSONObject) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                groupManager.updateGroupFromJson(group);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
            }).on("groupAdded", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject group = (JSONObject) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                groupManager.addGroupFromJson(group);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }
            }).on("groupRemoved", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject groupEvent = (JSONObject) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                groupManager.removeGroupById(groupEvent.getString("id"));
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });

                }
            }).on("callResult", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    final JSONObject callResult = (JSONObject) args[0];
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.v("socket", callResult.toString(2));
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                }


            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.v("disconnect", Arrays.toString(args));
                }

            })
                    .on(Socket.EVENT_ERROR, onConnectError)
                    .on(Socket.EVENT_CONNECT_ERROR, onConnectError)
                    .on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);


        } catch (
                URISyntaxException e
                )

        {
            e.printStackTrace();
        }


    }

    public void connect() {
        socket.connect();
    }

    public void call(String action, JSONObject params) throws JSONException {
        JSONObject call = new JSONObject();
        call.put("id", "some-id");
        call.put("action", action);
        call.put("params", params);
        socket.emit("call", call);
    }

    public void disconnect() {
        socket.disconnect();
    }

}
