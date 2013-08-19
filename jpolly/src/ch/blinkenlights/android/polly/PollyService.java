/*
 * Copyright (C) 2012 Adrian Ulrich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package ch.blinkenlights.android.polly;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.content.Intent;
import android.content.IntentFilter;

import android.telephony.TelephonyManager;
import android.media.AudioManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.ClassNotFoundException;
import java.lang.NoSuchMethodException;
import java.lang.reflect.Method;
import java.lang.Exception;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class PollyService extends Service {
	private final IBinder ply_binder        = new LocalBinder();
	private final String  rild_socket       = "/dev/socket/rild-audio-gsm";
	private final String  INTENT_VOLUME     = "android.media.VOLUME_CHANGED_ACTION"; /* sent if user changes any volume */
	private final String  INTENT_PHONESTATE = "android.intent.action.PHONE_STATE";   /* dialing, incall or hangup       */
	private final String  INTENT_AIRPLANE   = "android.intent.action.AIRPLANE_MODE"; /* switching from/to airplane mode */
	private AudioManager aMgr;
	private TelephonyManager tMgr;
	private int lastvol = -1;
    private boolean inCall = false;
    private static String TAG = "PollyService";
	
	@Override
	public IBinder onBind(Intent i) {
		return ply_binder;
	}
	
	public class LocalBinder extends Binder {
		public PollyService getService() {
			return PollyService.this;
		}
	}

	@Override
	public void onCreate() {
		aMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		tMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        // initial value
    	lastvol = aMgr.getStreamVolume(aMgr.STREAM_VOICE_CALL);

		registerReceiver(ply_receiver, new IntentFilter(INTENT_VOLUME));    /* undocumented */
		registerReceiver(ply_receiver, new IntentFilter(INTENT_AIRPLANE));
		registerReceiver(ply_receiver, new IntentFilter(INTENT_PHONESTATE));
	}
	
	public void onDestroy() {
		unregisterReceiver(ply_receiver);
	}
	
	
	private final BroadcastReceiver ply_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			String ia = intent.getAction();
			
			xlog("intent "+ia+" received");
			
			if(ia == INTENT_VOLUME)     { updateIncallVolume();       }
			if(ia == INTENT_PHONESTATE) { checkPhoneState();          }
			if(ia == INTENT_AIRPLANE)   { checkAirplaneState(intent); }
			
		}
		
		
		/**************************************************************
		** Fixup the volume on the modem - uses aMgr                 **
		**************************************************************/
		private void updateIncallVolume() {
			
			/* android volumes go from 0-5
			** but the modem expects 32-92
			*/
			int curvol    = aMgr.getStreamVolume(aMgr.STREAM_VOICE_CALL);
			
			if(lastvol != curvol) {
			    String vparam = "@volo,40,8,3,"+(32+curvol*13);
				xlog("updating incall volume: "+vparam);
				sendToPollySocket(vparam);
				lastvol = curvol;
			}
		}

		private void resetIncallVolume() {
			String vparam = "@@";
        	xlog("reset incall volume: "+vparam);
			sendToPollySocket(vparam);
		}
		
		/**************************************************************
		** Force a volume-set if we switched to incall mode         **
		** This is racy, but it seems that we always get called     **
		** after libcallvolume.so finished the initial setup        **
		**************************************************************/
		private void checkPhoneState() {
			if(tMgr.getCallState() == tMgr.CALL_STATE_OFFHOOK) {
				xlog("we are IN_CALL - forcing audio setting");
				lastvol = -1;
				updateIncallVolume();
				inCall = true;
			} else if(inCall && tMgr.getCallState() == tMgr.CALL_STATE_IDLE) {
				xlog("we are END_CALL - reset audio setting");
				resetIncallVolume();
				inCall = false;
			}
		}
		
		
		/**************************************************************
		** Tell pollyd that it should exit/restart itself            **
		** This is needed to re-connect to gsmmux                    **
		**************************************************************/
		private void checkAirplaneState(Intent intent) {
			boolean going_offline = intent.getExtras().getBoolean("state");
			
			if(going_offline == false) {
				xlog("exiting airplane state, restarting pollyd");
				sendToPollySocket("kill,DEAD_PARROT"); // from pollyd.h kill, -> dummy space (volo,)
			}
		}
		
		
		/**************************************************************
		** Sends given string to pollyd                              **
		**************************************************************/
		private void sendToPollySocket(String whatever) {
			LocalSocket lsock = new LocalSocket();
			try {
				lsock.connect(new LocalSocketAddress(rild_socket, LocalSocketAddress.Namespace.FILESYSTEM));
				lsock.getOutputStream().write(whatever.getBytes());
			}
			catch (Exception e) {
				xlog("sendToPollySocket "+e);
            }
			
			try {
				lsock.close();
			} catch(Exception e) {}
		}
		
		
		/**************************************************************
		** Send given string to android log                          **
		**************************************************************/
		private void xlog(String msg) {
			Log.d(TAG, msg);
		}
	};
}
