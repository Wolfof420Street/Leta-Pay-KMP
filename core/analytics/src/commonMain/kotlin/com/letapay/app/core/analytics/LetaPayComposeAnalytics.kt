/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:Suppress("SpreadOperator")

package com.letapay.app.core.analytics

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import template.core.base.analytics.AnalyticsHelper
import template.core.base.analytics.rememberAnalyticsHelper

/** Mifos-specific Compose analytics utilities */

/** Track Mifos screen views with additional business context */
@Composable
fun TrackLetaPayScreen(
    screenName: String,
    clientId: String? = null,
    loanId: String? = null,
    groupId: String? = null,
    additionalParams: Map<String, String> = emptyMap(),
) {
    val analytics = rememberAnalyticsHelper()
    val letapayTracker = remember(analytics) { analytics.letapayTracker() }

    LaunchedEffect(screenName) {
        val params = mutableMapOf<String, String>()
        clientId?.let { params[LetaPayParamKeys.CLIENT_ID] = it }
        loanId?.let { params[LetaPayParamKeys.LOAN_ID] = it }
        groupId?.let { params[LetaPayParamKeys.GROUP_ID] = it }
        params.putAll(additionalParams)

        analytics.logScreenView(screenName)
        if (params.isNotEmpty()) {
            analytics.logEvent("mifos_screen_context", params)
        }
    }
}

/** Track client-related button clicks */
fun Modifier.trackClientAction(
    action: String,
    clientId: String? = null,
): Modifier = this.then(
    Modifier.trackLetaPayAction(
        "client_action",
        mapOf(
            "action" to action,
            *clientId?.let { arrayOf(LetaPayParamKeys.CLIENT_ID to it) } ?: emptyArray(),
        ),
    ),
)

/** Track loan-related button clicks */
fun Modifier.trackLoanAction(
    action: String,
    loanId: String? = null,
    loanProductId: String? = null,
): Modifier = this.then(
    Modifier.trackLetaPayAction(
        "loan_action",
        mapOf(
            "action" to action,
            *loanId?.let { arrayOf(LetaPayParamKeys.LOAN_ID to it) } ?: emptyArray(),
            *loanProductId?.let { arrayOf(LetaPayParamKeys.LOAN_PRODUCT_ID to it) } ?: emptyArray(),
        ),
    ),
)

/** Track savings-related button clicks */
fun Modifier.trackSavingsAction(
    action: String,
    accountId: String? = null,
): Modifier = this.then(
    Modifier.trackLetaPayAction(
        "savings_action",
        mapOf(
            "action" to action,
            *accountId?.let { arrayOf(LetaPayParamKeys.SAVINGS_ACCOUNT_ID to it) } ?: emptyArray(),
        ),
    ),
)

/** Generic Mifos action tracker */
@Suppress("UnusedParameter")
private fun Modifier.trackLetaPayAction(
    eventType: String,
    params: Map<String, String>,
): Modifier = this.clickable {
    // Note: In a real implementation, you'd need to access the analytics helper here
    // This is a simplified version for demonstration
}

/** Track form field interactions in Mifos forms */
@Composable
fun TrackLetaPayFormField(
    fieldName: String,
    formName: String,
    fieldType: String = "text",
) {
    val analytics = rememberAnalyticsHelper()

    LaunchedEffect(fieldName, formName) {
        analytics.logEvent(
            "form_field_focused",
            "field_name" to fieldName,
            "form_name" to formName,
            "field_type" to fieldType,
        )
    }
}

/** Track Mifos business flow completion */
@Composable
fun TrackLetaPayFlowCompletion(
    flowName: String,
    step: String,
    totalSteps: Int,
    entityId: String? = null,
) {
    val analytics = rememberAnalyticsHelper()

    LaunchedEffect(step) {
        val params = mutableMapOf(
            "flow_name" to flowName,
            "current_step" to step,
            "total_steps" to totalSteps.toString(),
            "progress_percentage" to "${(step.toIntOrNull() ?: 0) * 100 / totalSteps}",
        )
        entityId?.let { params["entity_id"] = it }

        analytics.logEvent("mifos_flow_progress", params)
    }
}

/** Track navigation within Mifos workflows */
@Composable
fun TrackLetaPayNavigation(
    fromScreen: String,
    toScreen: String,
    navigationTrigger: String = "user_action",
    workflowName: String? = null,
) {
    val analytics = rememberAnalyticsHelper()

    LaunchedEffect(fromScreen, toScreen) {
        val params = mutableMapOf(
            "from_screen" to fromScreen,
            "to_screen" to toScreen,
            "trigger" to navigationTrigger,
        )
        workflowName?.let { params["workflow"] = it }

        analytics.logEvent("mifos_navigation", params)
    }
}

/** Track document operations in Mifos */
@Composable
fun rememberLetaPayDocumentTracker(): DocumentTracker {
    val analytics = rememberAnalyticsHelper()
    return remember(analytics) { DocumentTracker(analytics) }
}

class DocumentTracker(private val analytics: AnalyticsHelper) {
    fun trackUpload(documentType: String, fileSize: Long, success: Boolean = true) {
        analytics.trackDocumentOperation("upload", documentType, fileSize, success)
    }

    fun trackDownload(documentType: String, success: Boolean = true) {
        analytics.trackDocumentOperation("download", documentType, success = success)
    }

    fun trackView(documentType: String) {
        analytics.trackDocumentOperation("view", documentType, success = true)
    }
}

/** Track survey interactions in Mifos */
@Composable
fun TrackLetaPaySurvey(
    surveyId: String,
    // "started", "answered", "completed", "abandoned"
    action: String,
    questionId: String? = null,
) {
    val analytics = rememberAnalyticsHelper()

    LaunchedEffect(surveyId, questionId, action) {
        val params = mutableMapOf(
            LetaPayParamKeys.SURVEY_ID to surveyId,
            "survey_action" to action,
        )
        questionId?.let { params[LetaPayParamKeys.QUESTION_ID] = it }

        analytics.logEvent("mifos_survey_interaction", params)
    }
}

/** Track report generation in Mifos */
@Composable
fun rememberLetaPayReportTracker(): ReportTracker {
    val analytics = rememberAnalyticsHelper()
    return remember(analytics) { ReportTracker(analytics) }
}

class ReportTracker(private val analytics: AnalyticsHelper) {
    val analyticsTracker = LetaPayAnalyticsTracker(analytics)
    fun trackGeneration(
        reportName: String,
        filters: Map<String, String> = emptyMap(),
        duration: Long,
        success: Boolean = true,
    ) {
        analyticsTracker.trackReportGeneration(
            reportType = reportName,
            filterParams = filters,
            generationTime = duration,
            success = success,
        )
    }

    fun trackExport(reportName: String, format: String, success: Boolean = true) {
        analytics.logEvent(
            "report_exported",
            LetaPayParamKeys.REPORT_NAME to reportName,
            LetaPayParamKeys.EXPORT_FORMAT to format,
            "success" to success.toString(),
        )
    }

    fun trackShare(reportName: String, method: String) {
        analytics.logEvent(
            "report_shared",
            LetaPayParamKeys.REPORT_NAME to reportName,
            "share_method" to method,
        )
    }
}
