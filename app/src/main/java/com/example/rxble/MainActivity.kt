package com.example.rxble

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.rxble.ble.IBleController
import com.polidea.rxandroidble2.exceptions.BleException
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    val batteryLevelCommand = ByteArray(8).apply {
        this[0] = 0xAA.toByte()
        this[1] = 0x04.toByte()
        this[2] = 0x01.toByte()
        this[3] = 0xEB.toByte()
        this[4] = 0x00.toByte()
        this[5] = 0x9A.toByte()
        this[6] = 0xEB.toByte()
        this[7] = 0xAA.toByte()
    }

    val softwareNumber = ByteArray(8).apply {
        this[0] = 0xAA.toByte()
        this[1] = 0x04.toByte()
        this[2] = 0x01.toByte()
        this[3] = 0x76.toByte()
        this[4] = 0x00.toByte()
        this[5] = 0x25.toByte()
        this[6] = 0xEB.toByte()
        this[7] = 0xAA.toByte()
    }

    @Inject
    lateinit var bleController: IBleController

    private val disposable = CompositeDisposable()

    private val locationRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) bleController.establishConnection()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as BleApplication).appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bleController.observeConnectionState()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.w("BLE", "Connection state: $it")
                },
                {
                    Log.e("BLE", "Connection state error $it")
                }
            )
            .let { disposable.add(it) }

        findViewById<Button>(R.id.startScan).setOnClickListener {
            locationRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        findViewById<Button>(R.id.getBattery).setOnClickListener {
            bleController.getBattery()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
                    },
                    {
                        Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                )
                .let { disposable.add(it) }
        }

        findViewById<Button>(R.id.getSoftware).setOnClickListener {
            bleController.getSoftware()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    },
                    {
                        Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                )
                .let { disposable.add(it) }
        }

        findViewById<Button>(R.id.getAllData).setOnClickListener {
            bleController.getAllData()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
                    },
                    {
                        Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                )
                .let { disposable.add(it) }
        }

        findViewById<Button>(R.id.disconnect).setOnClickListener {
            bleController.disconnect()
        }
    }
}

data class MyModel(val battery: Int = 0, val software: String = "")

sealed class RxBleException : BleException() {
    class WrongDataSizeException : RxBleException()
}