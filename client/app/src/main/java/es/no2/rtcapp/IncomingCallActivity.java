package es.no2.rtcapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class IncomingCallActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_CALL = 123;

    private Button btnAcceptCall;
    private Button btnDeclineCall;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, IncomingCallActivity.class);
        activity.startActivityForResult(intent, REQUEST_CODE_CALL);
    }

    public static void launch(Context context) {
        Intent intent = new Intent(context, IncomingCallActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        initViews();
        setupClicks();
    }

    private void setupClicks() {
        btnAcceptCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptCall();
            }
        });
        btnDeclineCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                declineCall();
            }
        });
    }

    private void acceptCall() {
        setResult(RESULT_OK);
        finish();
    }


    private void declineCall() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void initViews() {
        btnAcceptCall = (Button) findViewById(R.id.btnAcceptCall);
        btnDeclineCall = (Button) findViewById(R.id.btnDeclineCall);
    }
}
