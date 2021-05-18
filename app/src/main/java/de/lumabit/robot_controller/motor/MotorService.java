package de.lumabit.robot_controller.motor;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;

public class MotorService extends Service {

    private final IBinder binder = new LocalBinder();

    private OutputStream mmOutStream;
    private double[] motorspeed = new double[]{0,0};

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public MotorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MotorService.this;
        }
    }

    @Override
    public void onCreate() {
        //Declare the timer
        Timer t = new Timer();
        //Set the schedule function and rate
        t.scheduleAtFixedRate(new TimerTask() {

            String actual = "";

            @Override
            public void run() {
                try {
                    String speed = "{" + (int) motorspeed[0] + "," + (int) motorspeed[1] + "}";
//                    console.setText(speed);
                    if (mmOutStream != null && actual != speed) {
                        mmOutStream.write(speed.getBytes());
                        actual = speed;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }, 0, 250);
    }

    private double modulo(double n, double m) {
        return (((n % m) + m) % m);
    }

    public void setOutStream(OutputStream mmOutStream) {
        this.mmOutStream = mmOutStream;
    }

    public void setSpeed(double strength, double angle) {
        motorspeed = calculateSpeed(strength, angle);
    }

    public void setSpeed(double[] speed) {
        motorspeed = speed;
    }

    private double[] calculateSpeed(double strength, double angle) {
        // assumes theta in degrees and r = 0 to 100 %
        // returns a tuple of percentages: (left_thrust, right_thrust)
        // def throttle_angle_to_thrust(r, theta):
        double theta = 360 - (angle >= 90 ? angle - 90 : angle + 270);
        double r = strength;
        theta = (modulo(theta + 180, 360)) - 180;  // normalize value to [-180, 180)
        r = Math.min(Math.max(0, r), 100);
        double v_a = r * (45 - modulo(theta, 90)) / 45;          // falloff of main motor
        double v_b = Math.min(100, Math.min(2 * r + v_a, 2 * r - v_a));  // compensation of other motor
        System.out.println("Theta " + theta);
        System.out.println("r " + r); // normalize value to [0, 100]
        System.out.println("v_a " + v_a);
        System.out.println("v_b " + v_b);

        if(theta < -90) return new double[]{-v_b, -v_a};
        if(theta < 0) return new double[]{-v_a, v_b};
        if(theta < 90)  return new double[]{v_b, v_a};
        return new double[]{v_a, -v_b};
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
