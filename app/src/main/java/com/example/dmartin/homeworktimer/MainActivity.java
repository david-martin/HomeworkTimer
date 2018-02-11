package com.example.dmartin.homeworktimer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.google.common.base.Stopwatch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private Manager manager;
    private Database database;

    private TextView timerTextView;
    private Stopwatch stopwatch = Stopwatch.createUnstarted();

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            millis = millis % 1000;

            timerTextView.setText(String.format("%02d:%02d:%03d", minutes, seconds, millis));

            timerHandler.postDelayed(this, 33);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            startDB();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        timerTextView = findViewById(R.id.timerTextView);

        // TODO: Bigger buttons
        Button timerButton = findViewById(R.id.timerButton);
        timerButton.setText("start");
        timerButton.setOnClickListener((view) -> {
            Button finishButton = findViewById(R.id.finishButton);
            finishButton.setEnabled(true);
            if (timerButton.getText().equals("pause")) {
                stopwatch.stop();
                timerHandler.removeCallbacks(timerRunnable);
                timerButton.setText("resume");

            } else {
                stopwatch.start();
                timerHandler.postDelayed(timerRunnable, 0);
                timerButton.setText("pause");
            }
        });

        Button historyButton = findViewById(R.id.historyButton);
        historyButton.setOnClickListener((view) -> {
            Intent historyIntent = new Intent(this, HistoryActivity.class);
            startActivity(historyIntent);
        });

        Button finishButton = findViewById(R.id.finishButton);
        finishButton.setText("finish");
        finishButton.setEnabled(false);
        finishButton.setOnClickListener((android.view.View view) -> {
            if (stopwatch.isRunning()) {
                stopwatch.stop();
            }
            long millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            stopwatch.reset(); // OK to reset now that we've taken a reading
            timerHandler.removeCallbacks(timerRunnable);
            timerButton.setText("start");

            TextView timerPreviousView = findViewById(R.id.timerPreviousView);
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            millis = millis % 1000;
            String previousValue = String.format("%02d:%02d:%03d", minutes, seconds, millis);
            timerPreviousView.setText(previousValue);
            // TODO: Include created_at date (formatted) in previous value text.
            try {
                createHistoryRecord(previousValue);
            } catch (CouchbaseLiteException e) {
                // TODO: More graceful way to handle db exceptions
                e.printStackTrace();
                System.exit(1);
            }


            TextView timerTextView = findViewById(R.id.timerTextView);
            timerTextView.setText(String.format("%02d:%02d:%03d", 0, 0, 0));

            Button button = (Button) view;
            button.setEnabled(false);
        });
    }

    private Document createHistoryRecord(String value) throws CouchbaseLiteException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        UUID uuid = UUID.randomUUID();
        Calendar calendar = GregorianCalendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        String id = currentTime + "-" + uuid.toString();

        Document document = database.createDocument();

        Map<String, Object> properties = new HashMap<>();
        properties.put("_id", id);
        // TODO: store raw history value instead of formatted string
        properties.put("text", value);
        properties.put("created_at", currentTimeString);
        document.putProperties(properties);

        Log.d("app", String.format("Created new value with id: %s", document.getId()));

        return document;

    }

    // TODO: Share DB manager & connection between activities?
    private void startDB() throws Exception {
        manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
        DatabaseOptions options = new DatabaseOptions();
        options.setCreate(true);
        database = manager.openDatabase("history", options);
    }
}
