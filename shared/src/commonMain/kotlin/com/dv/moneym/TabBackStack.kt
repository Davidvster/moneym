package com.dv.moneym

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey

internal class TabBackStack(startTab: NavKey) {

    private val stacks: LinkedHashMap<NavKey, SnapshotStateList<NavKey>> = linkedMapOf(
        startTab to mutableStateListOf(startTab)
    )

    private val _currentTab = mutableStateOf<NavKey>(startTab)
    var currentTab: NavKey
        get() = _currentTab.value
        private set(value) { _currentTab.value = value }

    val backStack: SnapshotStateList<NavKey> = mutableStateListOf(startTab)

    fun switchTab(tab: NavKey) {
        if (stacks[tab] == null) {
            stacks[tab] = mutableStateListOf(tab)
        } else {
            stacks.remove(tab)?.let { stacks[tab] = it }
        }
        currentTab = tab
        rebuildBackStack()
    }

    fun push(key: NavKey) {
        stacks[currentTab]?.add(key)
        rebuildBackStack()
    }

    fun removeLast() {
        val removed = stacks[currentTab]?.removeLastOrNull() ?: return
        if (removed == currentTab) {
            stacks.remove(currentTab)
            currentTab = stacks.keys.lastOrNull() ?: return
        }
        rebuildBackStack()
    }

    fun popTo(key: NavKey) {
        val stack = stacks[currentTab] ?: return
        while (stack.size > 1 && stack.last() != key) {
            stack.removeAt(stack.size - 1)
        }
        rebuildBackStack()
    }

    private fun rebuildBackStack() {
        backStack.clear()
        backStack.addAll(stacks.flatMap { (_, stack) -> stack })
    }
}
