package fedex.possessky.tyktwd;

import android.content.Intent;
import android.os.DeadObjectException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Results extends AppCompatActivity {


    TextView normScoreView, multView, scanNumView, finalScoreView;
    Button backToMain;

    private double multiplier = 1;
    private int scans;
    private int score;
    private double totalScore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        Intent thisIntent = getIntent();

        normScoreView = findViewById(R.id.userScore);
        multView = findViewById(R.id.accBonus);
        scanNumView = findViewById(R.id.scanNum);
        finalScoreView = findViewById(R.id.finalScore);

        backToMain = findViewById(R.id.menuBtn);

        String x = thisIntent.getStringExtra("finalScore");
        score = Integer.parseInt(x);
        int  mistakes= thisIntent.getIntExtra("mistakes", 0);
        scans = thisIntent.getIntExtra("totalScans", 0);


        if (mistakes == 0) {
            multiplier = 2.0;
        } else if (mistakes == 1) {
            multiplier = 1.5;
        } else if (mistakes > 1) {
            multiplier = 1;
        }

        totalScore = (score * multiplier) + scans;
        //method to update text
        setTextViews();


        //back to main menu, clears data.
        backToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });

    }




    private void setTextViews(){

        String baseScore = Integer.toString(score);
        String mult = Double.toString(multiplier);
        String finScore = Integer.toString((int)totalScore);
        String strScan = Integer.toString(scans);
        normScoreView.setText(baseScore);
        multView.setText(mult);
        scanNumView.setText(strScan);
        finalScoreView.setText(finScore);


    }
}
