package es.no2.rtcapp;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    private static final String SIGNALING_URI = "http://34.217.6.227:3000";
    private static final String VIDEO_TRACK_ID = "video1";
    private static final String AUDIO_TRACK_ID = "audio1";
    private static final String LOCAL_STREAM_ID = "stream1";
    private static final String SDP_MID = "sdpMid";
    private static final String SDP_M_LINE_INDEX = "sdpMLineIndex";
    private static final String SDP = "sdp";
    private static final String CREATEOFFER = "createoffer";
    private static final String OFFER = "offer";
    private static final String ANSWER = "answer";
    private static final String CANDIDATE = "candidate";
    private static final String CONNECT = "connect";
    private static final String TAG = "Text RTC";

    ArrayList<PeerConnection.IceServer> iceServers;
    DataChannel dataChannel;
    Button btnSendMsg, btnCall;
    GLSurfaceView videoView;
    private PeerConnectionFactory peerConnectionFactory;
    private VideoSource localVideoSource;
    private PeerConnection peerConnection;
    private MediaStream localMediaStream;
    private VideoRenderer otherPeerRenderer;
    private Socket socket;
    SessionDescription rsdp;

    PeerConnection.Observer peerConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d("RTCAPPLOG", "onSignalingChange:" + signalingState.toString());
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d("RTCAPPLOG", "onIceConnectionChange:" + iceConnectionState.toString());
            if(iceConnectionState.toString().equals("CLOSED"))
                disconnectCall();
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d("RTCAPPLOG", "onSetFailure:" + b);

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d("RTCAPPLOG", "onSetFailure:" + iceGatheringState.toString());

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            try {
                JSONObject obj = new JSONObject();
                obj.put(SDP_MID, iceCandidate.sdpMid);
                obj.put(SDP_M_LINE_INDEX, iceCandidate.sdpMLineIndex);
                obj.put(SDP, iceCandidate.sdp);
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(obj);
                socket.emit(CANDIDATE, jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            mediaStream.videoTracks.getFirst().addRenderer(otherPeerRenderer);
            Log.d("RTCAPPLOG", "onAddStream" + mediaStream.toString());

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d("RTCAPPLOG", "onRemoveStream" + mediaStream.toString());

        }

        @Override
        public void onDataChannel(final DataChannel dataChannel) {
            Log.d("RTCAPPLOG", "onDataChannel" + dataChannel.toString());
            dataChannel.registerObserver(new DataChannel.Observer() {
                @Override
                public void onBufferedAmountChange(long l) {

                }

                @Override
                public void onStateChange() {
                    Log.d(TAG, "onStateChange: remote data channel state: " + dataChannel.state().toString());
                }

                @Override
                public void onMessage(final DataChannel.Buffer buffer) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "onMessage: got message");
                            String message = byteBufferToString(buffer.data, Charset.defaultCharset());
                            Toast.makeText(MainActivity.this, "message: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });


        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d("RTCAPPLOG", "onRenegotiationNeeded");
        }
    };
    private boolean createOffer = false;
    SdpObserver sdpObserver = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d("RTCAPPLOG", "onCreateSuccess:" + sessionDescription.toString());

            peerConnection.setLocalDescription(sdpObserver, sessionDescription);
            try {
                JSONObject obj = new JSONObject();
                obj.put(SDP, sessionDescription.description);
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(obj);
                if (createOffer) {
                    socket.emit(OFFER, jsonArray);
                } else {
                    socket.emit(ANSWER, jsonArray);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSetSuccess() {
            Log.d("RTCAPPLOG", "onSetSuccess");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.d("RTCAPPLOG", "onCreateFailure:" + s);

        }

        @Override
        public void onSetFailure(String s) {
            Log.d("RTCAPPLOG", "onSetFailure:" + s);
        }
    };

    private void disableCalling() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnCall.setEnabled(true);
                btnCall.setText("End Call");
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        btnSendMsg = (Button) findViewById(R.id.btnSendMsg);
        btnCall = (Button) findViewById(R.id.btnCall);
        btnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCallClick();
            }
        });
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);

        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true); // Render EGL Context

        peerConnectionFactory = new PeerConnectionFactory();

        VideoCapturerAndroid vc = VideoCapturerAndroid.create(CameraEnumerationAndroid.getNameOfFrontFacingDevice(), null);

        localVideoSource = peerConnectionFactory.createVideoSource(vc, new MediaConstraints());
        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);
        localVideoTrack.setEnabled(true);

        AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(true);

        localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID);
        localMediaStream.addTrack(localVideoTrack);
        localMediaStream.addTrack(localAudioTrack);

        videoView = (GLSurfaceView) findViewById(R.id.glview_call);

        VideoRendererGui.setView(videoView, null);
        try {
            otherPeerRenderer = VideoRendererGui.createGui(0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
            VideoRenderer renderer = VideoRendererGui.createGui(50, 50, 50, 50, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
            localVideoTrack.addRenderer(renderer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        iceServers = new ArrayList<>();
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        onConnect();
    }

    private void onCallClick(){
        if(btnCall.getText().equals("Call"))
            makeCall();
        else
            disconnectCall();
    }

    private void makeCall() {
        createOffer = true;
        peerConnection.createOffer(sdpObserver, new MediaConstraints());
        btnCall.setText("Calling...");
        btnCall.setEnabled(false);
    }

    public void onConnect() {
        if (peerConnection != null)
            return;

        peerConnection = peerConnectionFactory.createPeerConnection(
                iceServers,
                new MediaConstraints(),
                peerConnectionObserver);
        initializeDataChannel();
        peerConnection.addStream(localMediaStream);

        try {
            socket = IO.socket(SIGNALING_URI);
            socket.on(CREATEOFFER, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            enableCalling();
                        }
                    });
                }

            }).on(OFFER, new Emitter.Listener() {

                @Override
                public void call(final Object... args) {
                    try {
                        //JSONObject obj = (JSONObject) args[0];
                        JSONArray objArray = (JSONArray) args[0];
                        JSONObject obj = objArray.getJSONObject(0);
                        rsdp = new SessionDescription(SessionDescription.Type.OFFER,
                                obj.getString(SDP));
                        onInComingCall();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }).on(ANSWER, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    try {
                        Log.d("RTCAPPLOG", "ON Answer");

                        //JSONObject obj = (JSONObject) args[0];
                        JSONArray objArray = (JSONArray) args[0];
                        JSONObject obj = objArray.getJSONObject(0);
                        SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER,
                                obj.getString(SDP));
                        peerConnection.setRemoteDescription(sdpObserver, sdp);
                        Log.d("desc", "OFFER description:" + sdp.description);
                        disableCalling();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }).on(CANDIDATE, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.d("RTCAPPLOG", "ON candidate");

                    try {
                        //JSONObject obj = (JSONObject) args[0];
                        JSONArray objArray = (JSONArray) args[0];
                        JSONObject obj = objArray.getJSONObject(0);
                        peerConnection.addIceCandidate(new IceCandidate(obj.getString(SDP_MID),
                                obj.getInt(SDP_M_LINE_INDEX),
                                obj.getString(SDP)));
                        Log.d("desc", "CANDIDATE description:" + obj.getString(SDP));

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }).on(CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Log.d("RTCAPPLOG", "ON CONNECT");
                }

            });

            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void enableCalling() {
        btnCall.setVisibility(View.VISIBLE);
    }

    public void onInComingCall() {
        IncomingCallActivity.launch(this);
    }

    public void onAcceptCall() {
        peerConnection.setRemoteDescription(sdpObserver, rsdp);
        Log.d("desc", "OFFER description:" + rsdp.description);
        peerConnection.createAnswer(sdpObserver, new MediaConstraints());
    }

    private void initializeDataChannel() {
        dataChannel = peerConnection.createDataChannel("sendDataChannel", new DataChannel.Init());
        dataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {

            }

            @Override
            public void onStateChange() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dataChannel.state() == DataChannel.State.OPEN) {
                            btnSendMsg.setEnabled(true);
                        } else {
                            btnSendMsg.setEnabled(false);
                        }
                    }
                });
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                // Incoming messages, ignore
                // Only outcoming messages used in this example
            }
        });
    }

    public void sendMessage() {
        String message = "My test message";
        ByteBuffer data = stringToByteBuffer(message, Charset.defaultCharset());
        dataChannel.send(new DataChannel.Buffer(data, false));
    }

    private ByteBuffer stringToByteBuffer(String msg, Charset charset) {
        return ByteBuffer.wrap(msg.getBytes(charset));
    }

    private String byteBufferToString(ByteBuffer buffer, Charset charset) {
        byte[] bytes;
        if (buffer.hasArray()) {
            bytes = buffer.array();
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        }
        return new String(bytes, charset);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == IncomingCallActivity.REQUEST_CODE_CALL &&resultCode == RESULT_OK)
            onAcceptCall();
    }

    public void disconnectCall(){
        finish();
    }
}
