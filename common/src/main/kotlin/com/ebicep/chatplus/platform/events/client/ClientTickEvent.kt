package com.ebicep.chatplus.platform.events.client

import com.ebicep.chatplus.platform.PlatformServices

object ClientTickEvent {
    object CLIENT_POST {
        fun register(listener: () -> Unit) {
            PlatformServices.get().registerClientTickPost(listener)
        }
    }
}
