package com.ebicep.chatplus.config

import com.ebicep.chatplus.platform.PlatformServices
import java.nio.file.Path

object ConfigDirectory {

    @JvmStatic
    fun getConfigDirectory(): Path {
        return PlatformServices.get().getConfigDirectory()
    }
}