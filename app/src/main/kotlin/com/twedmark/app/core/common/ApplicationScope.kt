package com.twedmark.app.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ApplicationScope(dispatchers: AppDispatchers) {
    val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
}