package com.example.rxble.ble

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.example.rxble.MyModel
import com.example.rxble.RxBleException
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleController @Inject constructor(private val bleScanner: IBleScanner) : IBleController {

    private val connectionDisposable = CompositeDisposable()
    private var bleConnection: RxBleConnection? = null
    private var deviceBondState: Int? = bleScanner.bleDevice?.bluetoothDevice?.bondState

    private val batteryLevelCommand = ByteArray(8).apply {
        this[0] = 0xAA.toByte()
        this[1] = 0x04.toByte()
        this[2] = 0x01.toByte()
        this[3] = 0xEB.toByte()
        this[4] = 0x00.toByte()
        this[5] = 0x9A.toByte()
        this[6] = 0xEB.toByte()
        this[7] = 0xAA.toByte()
    }

    private val softwareNumber = ByteArray(8).apply {
        this[0] = 0xAA.toByte()
        this[1] = 0x04.toByte()
        this[2] = 0x01.toByte()
        this[3] = 0x76.toByte()
        this[4] = 0x00.toByte()
        this[5] = 0x25.toByte()
        this[6] = 0xEB.toByte()
        this[7] = 0xAA.toByte()
    }

    override fun observeConnectionState(): Observable<RxBleConnection.RxBleConnectionState> {
        return bleScanner.scanDevices()
            .flatMap { device -> device.observeConnectionStateChanges().startWith(RxBleConnection.RxBleConnectionState.DISCONNECTED) }
    }

    override fun establishConnection() {
        if (bleConnection != null) return
        bleScanner.scanDevices()
            .flatMap { it.establishConnection(true) }
            .doOnNext { bleConnection = it }
            .flatMap {
                val completable = if (deviceBondState == BluetoothDevice.BOND_BONDED) {
                    Completable.complete()
                } else {
                    observePairingStatus()
                }
                completable.andThen(Observable.just(it))
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Unit
                },
                {
                    Log.e("ble", it.localizedMessage)
                    clearConnection()
                }
            )
            .let {
                connectionDisposable.add(it)
            }
    }

    private fun observePairingStatus(): Completable = bleConnection?.let {
        it.setupNotification(
            UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"),
            NotificationSetupMode.QUICK_SETUP
        )
            .flatMap { it }
            .firstOrError()
            .doOnSuccess { Log.w("ble","array: ${it.decodeToString()}") }
            .ignoreElement()
    } ?: Completable.error(NullPointerException("Ble connection is null."))

    override fun disconnect() {
        clearConnection()
        connectionDisposable.clear()
    }

    private fun clearConnection() {
        bleConnection = null
        bleScanner.bleDevice = null
    }

    override fun getBattery(): Single<Int> = getData(8, batteryLevelCommand)
        .map { bytes -> bytes[4].toInt() }

    override fun getSoftware(): Single<String> = getData(26, softwareNumber)
        .map {
            it.drop(4)
                .dropLast(4)
                .toByteArray()
                .decodeToString()
        }

    override fun getAllData(): Single<MyModel> = getBattery()
        .flatMap { battery ->
            getSoftware()
                .map { softwareNumber ->
                    MyModel(battery, softwareNumber)
                }
        }

    private fun getData(bufferSize: Int, command: ByteArray): Single<ByteArray> = Single.zip(
        setNotification(bufferSize),
        writeCommand(command)
    ) { source1, _ -> source1 }
        .flatMap {
            if (it.size == bufferSize) {
                Single.just(it)
            } else {
                Single.error(RxBleException.WrongDataSizeException())
            }
        }
        .retry { tries, error -> tries < 5 && error is RxBleException.WrongDataSizeException }

    private fun setNotification(bufferSize: Int): Single<ByteArray> {
        return bleConnection?.let {
            it.setupNotification(
                UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"),
                NotificationSetupMode.QUICK_SETUP
            )
                .flatMap { it }
                .flatMap { Observable.fromIterable(it.toMutableList()) }
                .buffer(2, TimeUnit.SECONDS, bufferSize)
                .firstOrError()
                .map { it.toByteArray() }
        } ?: Single.error(NullPointerException("Ble connection is null."))
    }

    private fun writeCommand(command: ByteArray): Single<ByteArray> =
        bleConnection?.writeCharacteristic(
            UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"),
            command
        ) ?: Single.error(NullPointerException("Ble connection is null."))
}