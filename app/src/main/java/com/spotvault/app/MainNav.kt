package com.spotvault.app

/** Bottom-bar destinations — order drives horizontal slide direction when switching tabs. */
enum class BottomNavDestination(val order: Int) {
    SETTINGS(0),
    VAULT(1)
}
