package com.android.bignerdranch.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollService extends IntentService {
    private static final String TAG = "PollService";
    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15);
    public static final String ACTION_SHOW_NOTIFICATION =
            "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE =
            "com.bignerdranch.android.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent intent = PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent
                .getService(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (isOn) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        QueryPreferences.setAlarmOn(context, isOn);
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent intent = PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent
                .getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    public PollService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!isNetworkAvailableAndConnected()) {
            return;
        }

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);
        List<GalleryItem> items;

        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos();
        } else {
            items = new FlickrFetchr().searchPhotos(query);
        }

        if (items.size() == 0) {
            return;
        }
        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(TAG, "Got on old result: " + resultId);
        } else {
            Log.i(TAG, "Got a new result: " + resultId);
            Log.i(TAG, "-----Notification start");

            String CHANNEL_ID = "channel_1";
            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);


            Notification.Builder builder = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification
                        .Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_menu_camera)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pi)
                        .setAutoCancel(true);
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        getString(R.string.new_pictures_title),
                        NotificationManager.IMPORTANCE_DEFAULT);


            } else {
                builder = new Notification
                        .Builder(this)
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pi)
                        .setAutoCancel(true);
            }


            Log.i(TAG, "-----Notification finished");

            showBackgroundNotification(0, builder.build());
        }

        QueryPreferences.setLastResultId(this, resultId);
    }

    private void showBackgroundNotification(int requestCode, Notification notification)
    {
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);
        sendOrderedBroadcast(i, PERM_PRIVATE, null, null,
                Activity.RESULT_OK, null, null);
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = connectivityManager.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable &&
                connectivityManager.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }
}
