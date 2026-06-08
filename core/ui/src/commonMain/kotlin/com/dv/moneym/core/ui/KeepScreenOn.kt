package com.dv.moneym.core.ui

import androidx.compose.runtime.Composable

/** Keeps the device screen awake while [enabled] is true; releases the hold on dispose. */
@Composable
expect fun KeepScreenOn(enabled: Boolean)
