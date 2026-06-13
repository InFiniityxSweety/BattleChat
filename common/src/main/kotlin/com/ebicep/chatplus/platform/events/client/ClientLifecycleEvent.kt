package com.ebicep.chatplus.platform.events.client

import com.ebicep.chatplus.platform.PlatformServices

object ClientLifecycleEvent {
    object CLIENT_LEVEL_LOAD {
        fun register(listener: () -> Unit) {
            PlatformServices.get().registerClientLevelLoad(listener)
        }
    }

    object CLIENT_STARTED {
        fun register(listener: () -> Unit) {
            PlatformServices.get().registerClientStarted(listener)
        }
    }

    object CLIENT_STOPPING {
        fun register(listener: () -> Unit) {
            PlatformServices.get().registerClientStopping(listener)
        }
    }
}
