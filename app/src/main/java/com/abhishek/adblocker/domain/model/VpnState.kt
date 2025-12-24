package com.abhishek.adblocker.domain.model

sealed class VpnState {
    data object Disconnected : VpnState()
    data object Connecting : VpnState()
    data object Connected : VpnState()
}
