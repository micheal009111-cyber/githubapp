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

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.channel.SubscriptionEventListener;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    // ============================================================
    // PUSHER SETTINGS
    // ============================================================

    private static final String PUSHER_KEY =
            "6360d9584aa0c6e5d240";

    private static final String PUSHER_CLUSTER =
            "ap2";

    private static final String CHANNEL_NAME =
            "chat-channel";

    private static final String EVENT_NAME =
            "new-text";


    // ============================================================
    // NOTIFICATION SETTINGS
    // ============================================================

    private static final String NOTIFICATION_CHANNEL_ID =
            "pusher_alerts";

    private static final int NOTIFICATION_PERMISSION_REQUEST =
            1001;


    private Pusher pusher;

    private TextView statusText;
    private TextView lastAlertText;


    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        statusText =
                findViewById(R.id.statusText);

        lastAlertText =
                findViewById(R.id.lastAlertText);


        // Create Android notification channel
        createNotificationChannel();


        // Request Android 13+ notification permission
        requestNotificationPermission();


        // Connect to Pusher
        connectToPusher();
    }


    // ============================================================
    // CONNECT TO PUSHER
    // ============================================================

    private void connectToPusher() {

        try {

            PusherOptions options =
                    new PusherOptions();

            options.setCluster(
                    PUSHER_CLUSTER
            );


            pusher =
                    new Pusher(
                            PUSHER_KEY,
                            options
                    );


            // ----------------------------------------------------
            // Subscribe to channel
            // ----------------------------------------------------

            Channel channel =
                    pusher.subscribe(
                            CHANNEL_NAME
                    );


            // ----------------------------------------------------
            // Listen for new-text
            // ----------------------------------------------------

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


            // ----------------------------------------------------
            // Connect
            // ----------------------------------------------------

            pusher.connect();


            statusText.setText(
                    "🟡 Connecting to Pusher..."
            );


        } catch (Exception e) {

            statusText.setText(
                    "❌ Pusher error: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }


    // ============================================================
    // HANDLE PUSHER MESSAGE
    // ============================================================

    private void handlePusherMessage(
            String data) {

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


            // ----------------------------------------------------
            // Update app screen
            // ----------------------------------------------------

            lastAlertText.setText(
                    "🔥 Last alert: "
                            + alertText
            );


            // ----------------------------------------------------
            // Show Android notification
            // ----------------------------------------------------

            showNotification(
                    text1,
                    text2
            );


        } catch (Exception e) {

            lastAlertText.setText(
                    "❌ Invalid Pusher data"
            );

            e.printStackTrace();
        }
    }


    // ============================================================
    // SHOW NOTIFICATION
    // ============================================================

    private void showNotification(
            String text1,
            String text2) {


        String title;


        if (!text2.isEmpty()) {

            title =
                    "🔥 " + text2;

        } else {

            title =
                    "🔥 Pusher Alert";
        }


        String message;


        if (!text1.isEmpty()) {

            message =
                    text1;

        } else {

            message =
                    "New alert received";
        }


        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        NOTIFICATION_CHANNEL_ID
                )


                        .setSmallIcon(
                                android.R.drawable
                                        .ic_dialog_info
                        )


                        .setContentTitle(
                                title
                        )


                        .setContentText(
                                message
                        )


                        .setStyle(
                                new NotificationCompat
                                        .BigTextStyle()
                                        .bigText(message)
                        )


                        .setPriority(
                                NotificationCompat
                                        .PRIORITY_HIGH
                        )


                        .setAutoCancel(
                                true
                        )


                        .setDefaults(
                                NotificationCompat
                                        .DEFAULT_ALL
                        );


        NotificationManagerCompat manager =
                NotificationManagerCompat.from(
                        this
                );


        // Android 13+ permission check
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {


            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission
                            .POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        }


        manager.notify(
                (int)
                        (System.currentTimeMillis()
                                & 0x7fffffff),
                builder.build()
        );
    }


    // ============================================================
    // CREATE NOTIFICATION CHANNEL
    // ============================================================

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


            channel.enableVibration(
                    true
            );


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


    // ============================================================
    // NOTIFICATION PERMISSION
    // ============================================================

    private void requestNotificationPermission() {


        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {


            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission
                            .POST_NOTIFICATIONS
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


    // ============================================================
    // DISCONNECT
    // ============================================================

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
