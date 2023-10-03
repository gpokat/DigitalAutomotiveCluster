package com.example.simplecluster


import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

class ClusterMainView : AppCompatActivity() {

    private val TAG = "SR.CLUSTORVIEW"
    private lateinit var car: Car
    private lateinit var mCarPropertyManager: CarPropertyManager
    private val REQUIRED_PERMISSIONS = arrayOf(
        Car.PERMISSION_CAR_INFO,
        Car.PERMISSION_ENERGY,
        Car.PERMISSION_SPEED
    )
    val PERMISSION_REQUEST_CODE = 777

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cluster_main_view)

        var missingPermissions = HashSet<String>()
        for (reqPerm in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    reqPerm
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermissions.add(reqPerm)
            }
        }
        if (missingPermissions.isNotEmpty()) {
            //Looks like there is a bug with when requestPermissions thread finalized and then returned to main UI thread:
            //android.view.WindowLeaked: Activity com.android.permissioncontroller.permission.ui.GrantPermissionsActivity has leaked window DecorView
            //Persist in Goldfish Android 12 API 31, in API 32 in default automotive google simulator.
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            startCarAndSpeedometer()
        }
    }

    override fun onResume() {
        if (::car.isInitialized && !car.isConnected && !car.isConnecting) {
            startCar()
        }
        super.onResume()
    }


    override fun onPause() {
        if (::car.isInitialized && car.isConnected) {
            car.disconnect()
        }
        super.onPause()
    }
    override fun onStop() {
        if (::car.isInitialized && car.isConnected) {
            car.disconnect()
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (::car.isInitialized && car.isConnected) {
            car.disconnect()
        }
        super.onDestroy()
    }

    fun getCarPropertyManager() = mCarPropertyManager
    private fun startCar() {
        car = Car.createCar(this)
        mCarPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var allGranted = true
        for ((i, perm) in permissions.withIndex()) {
            var res = grantResults[i]
            Log.i(TAG, "perm is: $perm granted status: $res")
            if (res == PackageManager.PERMISSION_DENIED) {
                allGranted = false
                break
            }
        }
        if (allGranted) {
            startCarAndSpeedometer()
        } else {
            val parentLayout = findViewById<View>(android.R.id.content)
            Snackbar.make(
                parentLayout,
                "Please check permissions in settings.",
                Snackbar.LENGTH_LONG
            )
                .setAction("Settings") {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                .show()
        }
    }

    private fun startCarAndSpeedometer(){
        startCar()
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(false)
            .replace(R.id.main_fragment_container, SensorsData())
            .addToBackStack(null)
            .commit()
    }
}