package com.ebicep.chatplus

import com.ebicep.chatplus.platform.PlatformServices

object ChatPlusPlatformInit {

    @Volatile
    private var initialized = false

    @JvmStatic
    fun platformInit() {
        if (initialized) {
            return
        }
        synchronized(this) {
            if (initialized) {
                return
            }
            PlatformServices.get().platformInit()
            initialized = true
        }
    }
}