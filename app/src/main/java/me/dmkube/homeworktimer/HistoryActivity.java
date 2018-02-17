package me.dmkube.homeworktimer;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends Activity {

    private Manager manager;
    private Database database;
    private SimpleDateFormat historyFormat = new SimpleDateFormat("EEEE dd MMM yyyy");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        try {
            startDB();
        } catch (Exception e) {
            // TODO: More graceful way to handle db exceptions
            e.printStackTrace();
            System.exit(1);
        }

        View viewItemsByDate = database.getView("history");

        // TODO: Is this kind of view mapping even needed? Is there a simple query that would suffice?
        // TODO: Sort by created date, newest first
        viewItemsByDate.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                Object createdAt = document.get("created_at");
                if (createdAt != null) {
                    emitter.emit(createdAt.toString(), null);
                }
            }
        }, "1.0");
        List<String> items = new ArrayList<>();
        Query query = viewItemsByDate.createQuery();

        QueryEnumerator queryResults = null;
        try {
            queryResults = query.run();
        } catch (CouchbaseLiteException e) {
            // TODO: More graceful way to handle db exceptions
            e.printStackTrace();
            System.exit(1);
        }
        while (queryResults.hasNext()) {
            QueryRow row = queryResults.next();
            Document doc = row.getDocument();
            Date date;
            try {
                date = MainActivity.dateFormatter.parse((String) doc.getProperty("created_at"));
            } catch (ParseException e) {
                e.printStackTrace();
                continue;
            }

            items.add(String.format(" %s - %s", historyFormat.format(date), doc.getProperty("text")));
        }

        Collections.reverse(items);
        ListAdapter historyListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);

        ListView historyListView = findViewById(R.id.historyListView);
        historyListView.setAdapter(historyListAdapter);
    }

    // TODO: Share DB manager & connection between activities?
    private void startDB() throws Exception {
        manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
        DatabaseOptions options = new DatabaseOptions();
        options.setCreate(true);
        database = manager.openDatabase("history", options);
    }

    // TODO: Is cleanup needed on destroy?

}
