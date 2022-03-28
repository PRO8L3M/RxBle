package com.example.rxble.ble

import bleshadow.javax.inject.Singleton
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Singleton
class BleScanner @Inject constructor(private val bleClient: RxBleClient) : IBleScanner {

    override var bleDevice: RxBleDevice? = null

    override fun scanDevices(): Observable<RxBleDevice> {
        val scanMode: ScanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        return bleClient.scanBleDevices(scanMode)
            .map { it.bleDevice }
            .filter { it.name?.startsWith("ZEISS", ignoreCase = true) ?: false }
            .take(1)
            .doOnNext { bleDevice = it }
            .timeout(10, TimeUnit.SECONDS)
    }
}