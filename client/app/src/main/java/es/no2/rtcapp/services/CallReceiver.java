package es.no2.rtcapp.services;

import android.content.Context;
import android.util.Log;
import java.util.Date;
import es.no2.rtcapp.IncomingCallActivity;

public class CallReceiver extends PhonecallReceiver {

    @Override
    protected void onIncomingCallStarted(Context ctx, String number, Date start) {
        Log.d("CALLLLLLLLLLING.....","CALLLLLLLLLLING.....");
        IncomingCallActivity.launch(ctx);
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
        Log.d("CALLLLLLLLLLING.....","CALLLLLLLLLLING.....");

    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
        Log.d("CALLLLLLLLLLING.....","CALLLLLLLLLLING.....");

    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
        Log.d("CALLLLLLLLLLING.....","CALLLLLLLLLLING.....");

    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start) {
        Log.d("CALLLLLLLLLLING.....","CALLLLLLLLLLING.....");

    }
}
