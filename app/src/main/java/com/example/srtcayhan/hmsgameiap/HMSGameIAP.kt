package com.example.srtcayhan.hmsgameiap

import android.app.Application
import com.huawei.hms.api.HuaweiMobileServicesUtil

class HMSGameIAP : Application() {

    override fun onCreate() {
        super.onCreate()
        HuaweiMobileServicesUtil.setApplication(this)
    }
}