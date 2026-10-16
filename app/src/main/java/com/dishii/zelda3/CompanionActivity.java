package com.dishii.zelda3;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

import org.libsdl.app.SDLControllerManager;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Hosts the MinimapView on the device's MAIN display when the game runs on the
 * secondary one (SecondScreenSwap = 1 in zelda3.ini). A Presentation can't be
 * used for this: the framework refuses to attach presentation windows to the
 * default display (InvalidDisplayException), so the swapped layout needs a real
 * activity. It has to stay focusable (see onCreate), so controller input that
 * reaches it is forwarded to the game window.
 */
public class CompanionActivity extends Activity {

    private static final String TAG = "Zelda3SecondScreen";

    // The game activity starts/finishes us and the DUMP debug broadcast needs
    // the live view; both run in this process, so a static instance is enough.
    static volatile CompanionActivity instance;

    private MinimapView minimap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Unlike the bottom-screen Presentation, this activity is focusable: it
            // owns the default display, and leaving that display with no focusable
            // window makes any input routed there ANR ("no focused window"). The
            // gamepad focus it takes is handed back in dispatchKeyEvent below.
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().getDecorView().setBackgroundColor(Color.BLACK);
            getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            minimap = new MinimapView(this);
            setContentView(minimap);
        } catch (Throwable e) {
            // shares the game's process, so dying here would take the game
            // down with it; drop the companion screen instead (issue #19)
            Log.e(TAG, "Companion screen failed", e);
            CrashLog.report(this, "companion screen", e);
            finish();
            return;
        }
        instance = this;
        Log.i(TAG, "Companion activity up on display "
                + getWindowManager().getDefaultDisplay().getDisplayId());
    }

    // Gamepad input that lands here belongs to the game. A controller isn't
    // tied to a display: Android sends its keys to whichever display is
    // focused, and this activity, started after the game, takes that focus
    // (on the RG DS / RG DS Plus the game panel has no touch, so the player
    // can't tap focus back). Making this window non-focusable instead ANRs
    // ("no focused window"), so hand the events to the game's own dispatch;
    // its window keeps its internal view focus, so SDL picks them up as usual.
    // Same approach as Dusklight's DuskCompanionActivity.
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isFromController(event.getDevice())) {
            Activity game = MainActivity.current();
            if (game != null && game.dispatchKeyEvent(event)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    // Sticks and triggers, same reasoning.
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (SDLControllerManager.isDeviceSDLJoystick(event.getDeviceId())
                && SDLControllerManager.handleJoystickMotionEvent(event)) {
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }

    // Only real controllers: back/volume from elsewhere keep their usual meaning.
    private static boolean isFromController(InputDevice device) {
        if (device == null) {
            return false;
        }
        int sources = device.getSources();
        return (sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                || (sources & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD;
    }

    @Override
    protected void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }

    /** Debug: render the current view into a PNG (triggered via adb broadcast). */
    void dumpToFile(File file) {
        if (minimap == null || minimap.getWidth() == 0) return;
        try {
            Bitmap b = Bitmap.createBitmap(minimap.getWidth(), minimap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            minimap.draw(new Canvas(b));
            try (FileOutputStream fos = new FileOutputStream(file)) {
                b.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            b.recycle();
            Log.i(TAG, "dumped " + file);
        } catch (Exception e) {
            Log.e(TAG, "dump failed", e);
        }
    }
}
