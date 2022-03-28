package com.example.rxble

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ScannerModule::class, ControllerModule::class])
interface ApplicationComponent {

    @Component.Builder
    interface Factory {

        fun build(): ApplicationComponent

        @BindsInstance
        fun application(application: Application): Factory
    }

    fun inject(activity: MainActivity)
}

class BleApplication : Application() {
    val appComponent = DaggerApplicationComponent.builder().application(this).build()
}