package fedex.possessky.tyktwd;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

public class StartPage extends AppCompatActivity {
    private Window window;
    private Button button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_page);
        //locks portrait orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        window = this.getWindow();
        window.setStatusBarColor(this.getResources().getColor(R.color.black));

        button = findViewById(R.id.button);
        //start game when button is clicked.
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mainStart = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainStart);
            }
        });
    }
}