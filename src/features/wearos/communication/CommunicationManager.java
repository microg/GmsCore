package features.wearos.communication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;

public class CommunicationManager {

    private final DataClient dataClient;
    private final Handler mainHandler;

    public CommunicationManager(Context context) {
        this.dataClient = Wearable.getDataClient(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void startCommunication() {
        dataClient.addListener(new DataClient.OnDataChangedListener() {
            @Override
            public void onDataChanged(DataEventBuffer dataEventBuffer) {
                for (DataEvent event : dataEventBuffer) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        // Handle data changes (notifications, control signals, etc.)
                        processData(event);
                    }
                }
                dataEventBuffer.release();
            }
        });
    }

    private void processData(DataEvent event) {
        // Process the data (e.g., control actions, notifications)
        mainHandler.post(() -> {
            // Implement logic for the received data
            // e.g., send notifications or control actions to the WearOS device
        });
    }
}
