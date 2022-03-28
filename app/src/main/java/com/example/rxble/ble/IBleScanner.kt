package com.example.rxble.ble

import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable

interface IBleScanner {
    var bleDevice: RxBleDevice?
    fun scanDevices(): Observable<RxBleDevice>
}