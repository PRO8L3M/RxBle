package com.example.rxble.ble

import com.example.rxble.MyModel
import com.polidea.rxandroidble2.RxBleConnection
import io.reactivex.Observable
import io.reactivex.Single

interface IBleController {

    fun establishConnection()
    fun observeConnectionState(): Observable<RxBleConnection.RxBleConnectionState>
    fun disconnect()
    fun getBattery(): Single<Int>
    fun getSoftware(): Single<String>
    fun getAllData(): Single<MyModel>
}