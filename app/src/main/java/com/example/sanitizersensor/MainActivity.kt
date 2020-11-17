package com.example.sanitizersensor
//Thread.sleep(5_000)
//use this code to wait for 5 seconds

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Vibrator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity: AppCompatActivity(), SensorEventListener {
    //secondary firebase
    val secondary = FirebaseDatabase.getInstance("https://bait2123-202010-03.firebaseio.com/")
    val lcdbkG = secondary.getReference("PI_03_CONTROL").child("lcdbkG");
    val lcdbkR = secondary.getReference("PI_03_CONTROL").child("lcdbkR");

    //primary firebase
    val primary: FirebaseDatabase = FirebaseDatabase.getInstance("https://solenoid-lock-f65e8.firebaseio.com/")
    val room: DatabaseReference = primary.getReference("Room")
    val sanitizer = primary.getReference("Room").child("Room1").child("SanitizerLeft")

    //variables
    var textview: TextView? = null
    var sanLeft:TextView? = null
    var sensorManager: SensorManager? = null
    var proximitySensor: Sensor? = null
    var isProximitySensorAvailable: Boolean? = null
    var vibrator: Vibrator? = null
    var Sanitizer: Int ?= null
    var limitSanitize: Int ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //accessing UI
        textview = findViewById<TextView>(R.id.textView)
        sanLeft = findViewById<TextView>(R.id.sanLeft)

        //read sanitizer available from firebase
        room.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //storing the amount into code variable
                var totalSanitizer = dataSnapshot.child("Room1").child("SanitizerLeft").value.toString().toInt();
                var limitSanitizer = dataSnapshot.child("Room1").child("noOfPax").value.toString().toInt();
                Sanitizer = totalSanitizer
                limitSanitize = limitSanitizer
                sanLeft!!.text = "$Sanitizer drops sanitizer left"

            }
            override fun onCancelled(error: DatabaseError) {
            }
        })

        //accessing the proximity sensor
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        if (sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            proximitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            isProximitySensorAvailable = true
        } else {
            textview!!.text = "Proximity sensor is not available"
            isProximitySensorAvailable = false
        }

    }

    //when sensor is changing value, we need update the output firebase and storage left firebase
    override fun onSensorChanged(sensorEvent: SensorEvent) {

        //when a hand is near
        if(sensorEvent.values[0].toDouble() == 0.0){
            //when sanitizer still available
            if(Sanitizer!! > 0){



                //update sanitizer left
                Sanitizer = Sanitizer!! -1
                //write to UI
                sanLeft!!.text = "$Sanitizer drops sanitizer left"
                textview!!.text = "Ps! Here's your sanitizer"
                //write reaction when sanitizer dispense to common resources database : Green Light
                lcdbkG.setValue("255")
                lcdbkR.setValue("0")
                //write to resources left firebase
                sanitizer.setValue(Sanitizer.toString())


                //to make a auto update value scene
                if(Sanitizer == 0){
                    Thread.sleep(5_000)
                    //no more sanitizer, set to Red Light
                    lcdbkR.setValue("255")
                    //send message to worker to refill sanitizer
                    //debugging
                }
            }
        }
        else{
            sanLeft!!.text = "No more sanitizer left"
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