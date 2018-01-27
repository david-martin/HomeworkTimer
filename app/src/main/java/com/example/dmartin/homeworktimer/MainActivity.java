package com.example.dmartin.homeworktimer;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

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

        timerTextView = findViewById(R.id.timerTextView);

        Button timerButton = findViewById(R.id.timerButton);
        timerButton.setText("start");
        timerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Button finishButton = findViewById(R.id.finishButton);
                finishButton.setEnabled(true);
                Button timerButton = (Button) v;
                if (timerButton.getText().equals("pause")) {
                    stopwatch.stop();
                    timerHandler.removeCallbacks(timerRunnable);
                    timerButton.setText("resume");
                } else {
                    stopwatch.start();
                    timerHandler.postDelayed(timerRunnable, 0);
                    timerButton.setText("pause");
                }
            }
        });

        Button finishButton = findViewById(R.id.finishButton);
        finishButton.setText("finish");
        finishButton.setEnabled(false);
        finishButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if (stopwatch.isRunning()) {
                    stopwatch.stop();
                }
                long millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                stopwatch.reset(); // OK to reset now that we've taken a reading
                timerHandler.removeCallbacks(timerRunnable);
                Button timerButton = findViewById(R.id.timerButton);
                timerButton.setText("start");

                TextView timerPreviousView = findViewById(R.id.timerPreviousView);
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                millis = millis % 1000;
                timerPreviousView.setText(String.format("%02d:%02d:%03d", minutes, seconds, millis));

                TextView timerTextView = findViewById(R.id.timerTextView);
                timerTextView.setText(String.format("%02d:%02d:%03d", 0, 0, 0));

                Button finishButton = (Button) view;
                finishButton.setEnabled(false);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        Button timerButton = findViewById(R.id.timerButton);
        timerButton.setText("resume");
    }
}
