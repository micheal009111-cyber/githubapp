package com.example.pusheralert;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String PUSHER_KEY =
            "6360d9584aa0c6e5d240";

    private static final String PUSHER_CLUSTER =
            "ap2";

    private static final String CHANNEL_NAME =
            "chat-channel";

    private static final String EVENT_NAME =
            "new-text";

    private static final String NOTIFICATION_CHANNEL_ID =
            "pusher_alerts";

    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;

    private Pusher pusher;

    private TextView statusText;
    private TextView lastAlertText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        lastAlertText = findViewById(R.id.lastAlertText);

        createNotificationChannel();
        requestNotificationPermission();
        connectToPusher();
    }

    private void connectToPusher() {

        try {

            PusherOptions options = new PusherOptions();
            options.setCluster(PUSHER_CLUSTER);

            pusher = new Pusher(
                    PUSHER_KEY,
                    options
            );

            /*
             * Connection listener
             */
            pusher.connect(
                    new ConnectionEventListener() {

                        @Override
                        public void onConnectionStateChange(
                                ConnectionStateChange change) {

                            runOnUiThread(() -> {

                                String state =
                                        change.getCurrentState()
                                                .toString();

                                statusText.setText(
                                        "Pusher status: " + state
                                );

                            });
                        }

                        @Override
                        public void onError(
                                String message,
                                String code,
                                Exception e) {

                            runOnUiThread(() -> {

                                statusText.setText(
                                        "❌ Pusher error\n"
                                                + message
                                                + "\nCode: "
                                                + code
                                );

                            });

                            if (e != null) {
                                e.printStackTrace();
                            }
                        }
                    },
                    ConnectionState.ALL
            );

            /*
             * Subscribe to channel
             */
            Channel channel =
                    pusher.subscribe(CHANNEL_NAME);

            /*
             * Subscription success
             */
            channel.bind(
                    "pusher:subscription_succeeded",
                    new SubscriptionEventListener() {

                        @Override
                        public void onEvent(
                                PusherEvent event) {

                            runOnUiThread(() -> {

                                statusText.setText(
                                        "🟢 Connected\n"
                                                + "Channel: "
                                                + CHANNEL_NAME
                                );

                            });
                        }
                    }
            );

            /*
             * Listen for our event
             */
            channel.bind(
                    EVENT_NAME,
                    new SubscriptionEventListener() {

                        @Override
                        public void onEvent(
                                PusherEvent event) {

                            String data =
                                    event.getData();

                            runOnUiThread(() ->
                                    handlePusherMessage(data)
                            );
                        }
                    }
            );

        } catch (Exception e) {

            statusText.setText(
                    "❌ Android error:\n"
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private void handlePusherMessage(String data) {

        try {

            JSONObject json =
                    new JSONObject(data);

            String text1 =
                    json.optString(
                            "text1",
                            ""
                    );

            String text2 =
                    json.optString(
                            "text2",
                            ""
                    );

            String alertText;

            if (!text2.isEmpty()) {
                alertText = text2;
            } else {
                alertText = text1;
            }

            lastAlertText.setText(
                    "🔥 Received:\n"
                            + "text1 = "
                            + text1
                            + "\n"
                            + "text2 = "
                            + text2
            );

            showNotification(
                    text1,
                    text2
            );

        } catch (Exception e) {

            lastAlertText.setText(
                    "❌ Invalid Pusher data:\n"
                            + data
            );

            e.printStackTrace();
        }
    }

    private void showNotification(
            String text1,
            String text2) {

        String title;

        if (!text2.isEmpty()) {
            title = "🔥 " + text2;
        } else {
            title = "🔥 Pusher Alert";
        }

        String message;

        if (!text1.isEmpty()) {
            message = text1;
        } else {
            message = "New alert received";
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        NOTIFICATION_CHANNEL_ID
                )
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_info
                        )
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(
                                new NotificationCompat
                                        .BigTextStyle()
                                        .bigText(message)
                        )
                        .setPriority(
                                NotificationCompat
                                        .PRIORITY_HIGH
                        )
                        .setAutoCancel(true)
                        .setDefaults(
                                NotificationCompat.DEFAULT_ALL
                        );

        NotificationManagerCompat manager =
                NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        }

        manager.notify(
                (int) (
                        System.currentTimeMillis()
                                & 0x7fffffff
                ),
                builder.build()
        );
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            NOTIFICATION_CHANNEL_ID,
                            "Pusher Alerts",
                            NotificationManager
                                    .IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "Notifications received from Pusher"
            );

            channel.enableVibration(true);

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }

    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission
                                        .POST_NOTIFICATIONS
                        },
                        NOTIFICATION_PERMISSION_REQUEST
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

        if (pusher != null) {

            try {
                pusher.disconnect();
            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }
}
