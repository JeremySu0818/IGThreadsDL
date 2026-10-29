package com.jeremysu0818.igthreadsdl.ui

import com.jeremysu0818.igthreadsdl.domain.download.DownloadRecord

/** Keeps only the latest retry responsible for returning from the queue. */
internal class RetryNavigation {
    private var generation = 0L

    fun begin(): Long = ++generation

    fun leaveQueue() {
        generation++
    }

    fun isCurrent(token: Long): Boolean = token == generation

    fun historyTargetId(
        token: Long,
        tab: MainTab,
        originalId: Long,
        retriedId: Long,
        records: List<DownloadRecord>,
    ): Long? {
        if (!isCurrent(token) || tab != MainTab.QUEUE) return null
        val retried = records.firstOrNull { it.managerId == retriedId } ?: return null
        if (retried.isActive) return null
        return records.firstOrNull { it.managerId == originalId && !it.isActive }?.managerId
            ?: retried.managerId
    }
}
