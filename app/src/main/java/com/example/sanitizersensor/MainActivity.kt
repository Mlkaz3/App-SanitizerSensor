package com.example.sanitizersensor
//Thread.sleep(5_000)
//use this code to wait for 5 seconds

//TO DO LIST
//refresh the sanitizerUsed once the user logout/ stop booking
//msg the worker if sanitizer is out of stock
//the problem is top up dy no changes taken in the red light Ops : this bug solve

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Vibrator
import android.telephony.SmsManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.database.*


class MainActivity: AppCompatActivity(), SensorEventListener {
    //secondary firebase
    val secondary = FirebaseDatabase.getInstance("https://bait2123-202010-03.firebaseio.com/")
    val lcdbkG = secondary.getReference("PI_03_CONTROL").child("lcdbkG");
    val lcdbkR = secondary.getReference("PI_03_CONTROL").child("lcdbkR");
    val lcdbkB = secondary.getReference("PI_03_CONTROL").child("lcdbkB");
    val lcdtxt = secondary.getReference("PI_03_CONTROL").child("lcdtxt");

    //primary firebase
    val primary: FirebaseDatabase = FirebaseDatabase.getInstance("https://solenoid-lock-f65e8.firebaseio.com/")
    val room: DatabaseReference = primary.getReference("Room")
    val sanitizer = primary.getReference("Room").child("Room1").child("sanitizerLeft")
    val sanitizerUsed = primary.getReference("Room").child("Room1").child("sanitizerUsed")

    //variables
    var textview: TextView? = null
    var sanLeft:TextView? = null
    var sensorManager: SensorManager? = null
    var proximitySensor: Sensor? = null
    var isProximitySensorAvailable: Boolean? = null
    var vibrator: Vibrator? = null
    var Sanitizer: Int ?= null
    var limitSanitize: Int ?= null
    var userSanitize:Int ?=null
    var finish:MediaPlayer ?=null
    var limit:MediaPlayer ?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //accessing UI
        textview = findViewById<TextView>(R.id.textView)
        sanLeft = findViewById<TextView>(R.id.sanLeft)

        //music to be play
        finish = MediaPlayer.create(this, R.raw.finish);
        limit = MediaPlayer.create(this, R.raw.limit);

        //initialise firebase value: this code should be move to somewhere else, cant impossible every time we run the application, the led restart
        lcdbkG.setValue("0")
        lcdbkR.setValue("0")
        lcdbkB.setValue("0")

        //read sanitizer available from firebase
        room.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_SMS
                    ),
                    PackageManager.PERMISSION_GRANTED
                )
                //storing the amount into code variable
                var totalSanitizer = dataSnapshot.child("Room1").child("sanitizerLeft").value.toString().toInt();
                var limitSanitizer = dataSnapshot.child("Room1").child("noOfPax").value.toString().toInt();
                var usedSanitizer  = dataSnapshot.child("Room1").child("sanitizerUsed").value.toString().toInt();
                Sanitizer = totalSanitizer
                limitSanitize = limitSanitizer
                userSanitize = usedSanitizer
                sanLeft!!.text = "$Sanitizer drops sanitizer left"

                //if the sanitizer is top up dy: chg the red light to no red light
                if(Sanitizer!! > 0){
                    //set the red light to zero
                    lcdbkR.setValue("0")

                    //if the no of pax and limited sanitizer is not matched
                    if(limitSanitize!! > userSanitize!!){
                        lcdbkB.setValue("0")
                    }
                    else {
                        lcdbkB.setValue("255")
                    }
                }
                else{
                    lcdbkR.setValue("255")
                    lcdbkG.setValue("0")
                    lcdbkB.setValue("0")
                }

                lcdtxt.setValue("SANITIZERLEFT=" + String.format("%02d", Sanitizer))
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
            userSanitize = userSanitize?.plus(1)

            //when sanitizer still available
            if(Sanitizer!! > 0){

                //check if the user use more than limit
                //if yes: play limit()
                if(userSanitize!! > this!!.limitSanitize!!){
                    playlimit()
                    //blue light shows if they use too much
                    lcdbkB.setValue("255")
                }

                //if no: continue to check stock ability
                else{
                    //update sanitizer left
                    Sanitizer = Sanitizer!! -1
                    //write to UI
                    textview!!.text = "Ps! Here's your sanitizer"
                    sanLeft!!.text = "$Sanitizer drops sanitizer left"
                    //write reaction when sanitizer dispense to common resources database : Green Light
                    lcdbkG.setValue("255")
                    lcdbkR.setValue("0")
                    lcdbkB.setValue("0")
                    //write data to firebase
                    sanitizer.setValue(Sanitizer.toString())
                    sanitizerUsed.setValue(userSanitize.toString())
                    //inform the people :) idk how to do yet

                    if(userSanitize!! == this!!.limitSanitize!!){
                        //take 3 seconds to update the sanitizer
                        Thread.sleep(3_000)
                        //blue light shows if they use too much
                        lcdbkB.setValue("255")
                    }
                }
            }
            else{
                lcdbkR.setValue("255")
                sanLeft!!.text = "No more sanitizer left"
                playFinish()
            }
        }

        else{
            textview!!.text = "No human detected."
            lcdbkG.setValue("0")
        }
    }

//    private fun sendMail() {
//        try {
//            val sender = GMailSender("username@gmail.com", "password")
//            sender.sendMail(
//                "This is Subject",
//                "This is Body",
//                "user@gmail.com",
//                "user@yahoo.com"
//            )
//        } catch (e: Exception) {
//            Log.e("SendMail", e.message, e)
//        }
//    }


    private fun sendMsg() {
        //source
        //https://www.thecodecity.com/2017/07/send-email-from-android-app-directly.html
        val message:String = "The sanitizer is finished."
        val number:String ="60162635833"

        val mySmsManager: SmsManager = SmsManager.getDefault()
        mySmsManager.sendTextMessage(number, null, message, null, null)
    }

    private fun playFinish() {
        finish?.start()
    }

    private fun playlimit() {
        limit?.start();
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