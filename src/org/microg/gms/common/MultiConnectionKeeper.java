package org.microg.gms.common;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.microg.gms.Constants.GMS_PACKAGE_NAME;

public class MultiConnectionKeeper {
    private static MultiConnectionKeeper INSTANCE;

    private final Context context;
    private final Map<String, Connection> connections = new HashMap<>();

    public MultiConnectionKeeper(Context context) {
        this.context = context;
    }

    public static MultiConnectionKeeper getInstance(Context context) {
        if (INSTANCE == null)
            INSTANCE = new MultiConnectionKeeper(context);
        return INSTANCE;
    }

    public boolean bind(String action, ServiceConnection connection) {
        Connection con = connections.get(action);
        if (con != null) {
            if (!con.forwardsConnection(connection)) {
                con.addConnectionForward(connection);
                if (!con.isBound())
                    con.bind();
            }
        } else {
            con = new Connection(action);
            con.addConnectionForward(connection);
            con.bind();
            connections.put(action, con);
        }
        return con.isBound();
    }
    
    public void unbind(String action, ServiceConnection connection) {
        Connection con = connections.get(action);
        if (con != null) {
            con.removeConnectionForward(connection);
            if (!con.hasForwards() && con.isBound()) {
                con.unbind();
            }
        }
    }

    public class Connection {
        private final String actionString;
        private final Set<ServiceConnection> connectionForwards = new HashSet<>();
        private boolean bound = false;
        private boolean connected = false;
        private IBinder binder;
        private ComponentName component;
        private ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                binder = iBinder;
                component = componentName;
                for (ServiceConnection connection : connectionForwards) {
                    connection.onServiceConnected(componentName, iBinder);
                }
                connected = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                binder = null;
                component = componentName;
                for (ServiceConnection connection : connectionForwards) {
                    connection.onServiceDisconnected(componentName);
                }
                connected = false;
            }
        };

        public Connection(String actionString) {
            this.actionString = actionString;
        }

        public void bind() {
            Intent intent = new Intent(actionString).setPackage(GMS_PACKAGE_NAME);
            bound = context.bindService(intent, serviceConnection,
                    Context.BIND_ADJUST_WITH_ACTIVITY & Context.BIND_AUTO_CREATE);
            if (!bound) {
                context.unbindService(serviceConnection);
            }
        }

        public boolean isBound() {
            return bound;
        }

        public IBinder getBinder() {
            return binder;
        }

        public void unbind() {
            context.unbindService(serviceConnection);
            bound = false;
        }

        public void addConnectionForward(ServiceConnection connection) {
            connectionForwards.add(connection);
            if (connected) {
                connection.onServiceConnected(component, binder);
            }
        }

        public void removeConnectionForward(ServiceConnection connection) {
            connectionForwards.remove(connection);
            if (connected) {
                connection.onServiceDisconnected(component);
            }
        }

        public boolean forwardsConnection(ServiceConnection connection) {
            return connectionForwards.contains(connection);
        }
        
        public boolean hasForwards() {
            return !connectionForwards.isEmpty();
        }
    }
}
