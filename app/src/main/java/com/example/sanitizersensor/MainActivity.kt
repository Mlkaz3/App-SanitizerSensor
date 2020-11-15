package com.example.sanitizersensor
//Thread.sleep(5_000)
//use this code to wait for 5 seconds
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Vibrator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase



class MainActivity: AppCompatActivity(), SensorEventListener {
    var textview: TextView? = null
    var sanLeft:TextView? = null
    var sensorManager: SensorManager? = null
    var proximitySensor: Sensor? = null
    var isProximitySensorAvailable: Boolean? = null
    var vibrator: Vibrator? = null
    var sanitizertotalAmount:Int = 5

    val secondary = FirebaseDatabase.getInstance("https://bait2123-202010-03.firebaseio.com/")
    val lcdbkG = secondary.getReference("PI_03_CONTROL").child("lcdbkG");
    val lcdbkR = secondary.getReference("PI_03_CONTROL").child("lcdbkR");

    //write to our firebase
    val primary = FirebaseDatabase.getInstance("https://solenoid-lock-f65e8.firebaseio.com/")
    val sanitizer: DatabaseReference = primary.getReference("Room").child("Room1").child("sanitizerLeft");

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //accessing the proximity sensor
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        textview = findViewById<TextView>(R.id.textView)
        sanLeft = findViewById<TextView>(R.id.sanLeft)
        sanLeft!!.text = "$sanitizertotalAmount drops sanitizer left"

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            proximitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            isProximitySensorAvailable = true
        } else {
            textview!!.text = "Proximity sensor is not available"
            isProximitySensorAvailable = false
        }

        //initialize the sanitizer left as 30 drops
        sanitizer.setValue(sanitizertotalAmount.toString())


    }

    //when sensor is changing value, we need update the output firebase and storage left firebase
    override fun onSensorChanged(sensorEvent: SensorEvent) {
        //when a hand is near
        if(sensorEvent.values[0].toDouble() == 0.0){

            //when sanitizer still available
            if(sanitizertotalAmount > 1){
                //update sanitizer left
                sanitizertotalAmount -= 1
                //write to UI
                sanLeft!!.text = "$sanitizertotalAmount drops sanitizer left"
                textview!!.text = "Ps! Here's your sanitizer"
                //write reaction when sanitizer dispense to common resources database : Green Light
                lcdbkG.setValue("255")
                //write to resources left firebase
                sanitizer.setValue(sanitizertotalAmount.toString())

            }
            else{

                //write to UI
                sanLeft!!.text = "No more sanitizer left"
                //no more sanitizer, set to Red Light
                lcdbkR.setValue("255")
                //write to resources left firebase
                sanitizer.setValue("Please Re-fill")
                //send message to worker to refill sanitizer
                //debugging
            }


        }
        else{
            textview!!.text = "No human detected."
            lcdbkG.setValue("0")
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onPostResume() {
        super.onPostResume()
        if (isProximitySensorAvailable!!) {
            sensorManager!!.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }



}