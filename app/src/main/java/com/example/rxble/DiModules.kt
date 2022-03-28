package com.example.rxble

import android.app.Application
import com.example.rxble.ble.BleController
import com.example.rxble.ble.BleScanner
import com.example.rxble.ble.IBleController
import com.example.rxble.ble.IBleScanner
import com.polidea.rxandroidble2.RxBleClient
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
interface ScannerModule {

    @Module
    companion object {

        @Provides
        fun provideRxBleClient(application: Application): RxBleClient = RxBleClient.create(application.applicationContext)
    }

    @Binds
    fun bindScanner(bleScanner: BleScanner): IBleScanner
}

@Module
interface ControllerModule {

    @Binds
    fun bindController(bleController: BleController): IBleController

}
