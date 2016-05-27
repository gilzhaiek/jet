package com.example.hudmetricsclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

    @SuppressWarnings("unused")
    private static final String TAG = "HUDMetricsClientActivity";

    private Button mAltButton;
    private Button mSpeedButton;
    private Button mGradeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAltButton = (Button) findViewById(R.id.buttonAlt);
        mSpeedButton = (Button) findViewById(R.id.buttonSpeed);
        mGradeButton = (Button) findViewById(R.id.buttonGrade);
        mAltButton.setOnClickListener(this);
        mSpeedButton.setOnClickListener(this);
        mGradeButton.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case R.id.buttonAlt:
                Intent altIntent = new Intent(getApplicationContext(), AltitudeActivity.class);
                startActivity(altIntent);
                break;
            case R.id.buttonSpeed:
                Intent speedIntent = new Intent(getApplicationContext(), SpeedActivity.class);
                startActivity(speedIntent);
                break;
            case R.id.buttonGrade:
                Intent gradeIntent = new Intent(getApplicationContext(), GradeActivity.class);
                startActivity(gradeIntent);
                break;
        }
    }
}
