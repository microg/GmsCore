/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wallet.activity

import android.util.Log
import org.microg.vending.billing.proto.*
import kotlin.collections.iterator

/**
 * Event engine: trigger/condition/resultingAction indexing + broadcast
 *
 * Builds conditionGraph / resultingActionGraph index tables
 * Broadcast: per-target independent condition evaluation
 * Condition evaluation: VALUE_MATCH (type=1)
 * Action execution:
 *   233806715 DataResultingAction     — STATE_CHANGE / ENABLEMENT_CHANGE
 *   233780160 InfrastructureAction    — SUBMIT / FINISH
 *   265929774 AnimatedImageAction     — animation state (NOT_STARTED/RUNNING/COMPLETED)
 *   238549017 ConditionValueAction    — condValue change → UI branch switch
 */
class EventEngine {

    companion object {
        private const val TAG = "EventEngine"
    }

    private var conditionGraph = mutableMapOf<Long, MutableSet<Long>>()
    private var resultingActionGraph = mutableMapOf<Long, MutableSet<Long>>()

    private var components = mutableMapOf<Long, PageElement>()

    private var executionStates = mutableMapOf<Long, FunctionalDataExecutionState>()

    // 0=UNKNOWN, 1=NOT_STARTED, 2=RUNNING, 3=COMPLETED
    private var animatedImageStates = mutableMapOf<Long, Int>()

    /**
     * Build conditionGraph / resultingActionGraph index tables from all PageElements
     */
    fun rebuildGraphs(componentMap: Map<Long, PageElement>) {
        conditionGraph.clear()
        resultingActionGraph.clear()
        components.clear()
        executionStates.clear()

        for ((componentId, pe) in componentMap) {
            components[componentId] = pe

            // Record the initial execution state
            val state = pe.dataValue?.dataState?.executionState ?: FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_UNKNOWN
            executionStates[componentId] = state

            for (condition in pe.conditionList) {
                for (dataId in condition.dataIds) {
                    conditionGraph.getOrPut(dataId) { mutableSetOf() }.add(componentId)
                }
            }

            for (action in pe.resultingActionList) {
                for (dataId in action.dataIds) {
                    resultingActionGraph.getOrPut(dataId) { mutableSetOf() }.add(componentId)
                }
            }
        }

        Log.d(TAG, "rebuildGraphs: ${components.size} components, condGraph=${conditionGraph.keys}, actionGraph=${resultingActionGraph.keys}")
    }

    fun onComponentCompleted(completedComponentId: Long): List<ActionResult> {
        executionStates[completedComponentId] = FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_COMPLETED

        val pe = components[completedComponentId] ?: return emptyList()

        val trigger = pe.triggerList.firstOrNull() ?: return emptyList()
        val dataIds = trigger.dataIds
        if (dataIds.isEmpty()) return emptyList()

        Log.d(TAG, "onComponentCompleted: id=$completedComponentId, broadcasting dataIds=$dataIds")
        return broadcast(dataIds)
    }

    fun onButtonClick(dataIds: List<Long>): List<ActionResult> {
        Log.d(TAG, "onButtonClick: dataIds=$dataIds")
        return broadcast(dataIds)
    }

    private fun broadcast(dataIds: List<Long>): List<ActionResult> {
        val results = mutableListOf<ActionResult>()

        for (dataId in dataIds) {
            // Process each resultingAction target subscribed to this dataId independently
            val actionTargets = resultingActionGraph[dataId] ?: continue
            for (targetCid in actionTargets) {
                val targetPe = components[targetCid] ?: continue

                // Per-target condition check: only check the conditions that this target itself has subscribed to for this dataId
                val relevantConditions = targetPe.conditionList.filter { dataId in it.dataIds }
                var targetSatisfied = true
                for (condition in relevantConditions) {
                    val satisfied = evaluateCondition(targetCid, condition)
                    val negated = condition.negated ?: false
                    val finalResult = if (negated) !satisfied else satisfied
                    if (!finalResult) {
                        Log.d(TAG, "broadcast: dataId=$dataId target=$targetCid blocked by condition")
                        targetSatisfied = false
                        break
                    }
                }

                if (!targetSatisfied) continue

                // Condition satisfied → execute the resultingActions that this target has subscribed to for this dataId
                val matchingActions = targetPe.resultingActionList.filter { dataId in it.dataIds }
                for (action in matchingActions) {
                    val result = executeAction(targetCid, action)
                    if (result != null) {
                        results.add(result)
                        // After RUN_VALIDATION passes, chain into followUpTrigger.dataIds
                        // so the downstream AES components (subscribed to followUp dataIds)
                        // get notified to start encryption.
                        if (result is ActionResult.ValidationPassed) {
                            val followUpIds = action.followUpTrigger?.dataIds.orEmpty()
                            if (followUpIds.isNotEmpty()) {
                                Log.d(TAG, "broadcast: followUpTrigger from cid=$targetCid → dataIds=$followUpIds")
                                results.addAll(broadcast(followUpIds))
                            }
                        }
                    }
                }
            }
        }

        return results
    }

    private fun evaluateCondition(componentId: Long, condition: ConditionRule): Boolean {
        val dsc = condition.dataStateCondition ?: return true
        val matchType = dsc.matchType ?: return true

        return when (matchType) {
            1 -> {
                val targetState = FunctionalDataExecutionState.fromValue(dsc.matchValue ?: 0)
                    ?: FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_UNKNOWN
                val currentState = executionStates[componentId]
                    ?: FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_UNKNOWN
                val result = currentState == targetState
                Log.d(TAG, "evaluateCondition: id=$componentId, current=$currentState, target=$targetState, result=$result")
                result
            }
            else -> {
                Log.d(TAG, "evaluateCondition: unsupported matchType=$matchType, returning true")
                true
            }
        }
    }

    /**
     * Execute action: dispatch based on the resultingAction extension type
     */
    private fun executeAction(targetCid: Long, action: ResultingActionRule): ActionResult? {
        // Check for DataResultingAction (233806715): change state
        action.dataResultingAction?.let { dra ->
            when (dra.resultActionType) {
                DataResultingAction.ResultingActionType.RESULTING_ACTION_TYPE_ENABLEMENT_STATE_CHANGE -> {
                    val newEnablement = dra.enablementState ?: 0
                    Log.d(TAG, "executeAction: id=$targetCid ENABLEMENT_CHANGE → $newEnablement")
                    // enablementState: 1=ENABLED, 2=DISABLED
                    return ActionResult.EnablementChange(targetCid, newEnablement)
                }
                DataResultingAction.ResultingActionType.RESULTING_ACTION_TYPE_FUNCTIONAL_DATA_EXECUTION_STATE_CHANGE -> {
                    val newState = dra.executionState
                        ?: return null
                    Log.d(TAG, "executeAction: id=$targetCid STATE_CHANGE → $newState")
                    executionStates[targetCid] = newState
                    return ActionResult.StateChange(targetCid, newState)
                }
                DataResultingAction.ResultingActionType.RESULTING_ACTION_TYPE_RUN_VALIDATION -> {
                    // Input-field validation. We always pass (the field has text;
                    // a real GMS impl would also evaluate regex etc.). Returning
                    // ValidationPassed lets broadcast() chain into followUpTrigger.
                    Log.d(TAG, "executeAction: id=$targetCid RUN_VALIDATION → pass")
                    return ActionResult.ValidationPassed(targetCid)
                }

                else -> {
                    Log.d(TAG, "executeAction: id=$targetCid unhandled dataResultingAction type=${dra.resultActionType}")
                }
            }
        }

        // Check for AnimatedImageAction (265929774): change AnimatedImage state
        action.animatedImageAction?.let { aia ->
            val newState = aia.animatedImageState ?: 0
            Log.d(TAG, "executeAction: id=$targetCid ANIMATED_IMAGE_STATE → $newState (0=UNKNOWN,1=NOT_STARTED,2=RUNNING,3=COMPLETED)")
            animatedImageStates[targetCid] = newState
            return ActionResult.AnimatedImageStateChange(targetCid, newState)
        }

        // Check for ConditionValueAction (238549017): change condValue → UI switch
        // Changes the condValue of the condition container after a button click
        action.conditionValueAction?.let { cva ->
            val newValue = cva.conditionValue ?: 0
            Log.d(TAG, "executeAction: id=$targetCid CONDITION_VALUE_CHANGE → $newValue")
            return ActionResult.ConditionValueChange(targetCid, newValue)
        }

        //SUBMIT/FINISH
        action.infrastructureAction?.let { ia ->
            return when (ia.resultActionType) {
                ResultingActionType.RESULTING_ACTION_TYPE_SUBMIT -> {
                    Log.d(TAG, "executeAction: id=$targetCid SUBMIT")
                    ActionResult.Submit
                }
                ResultingActionType.RESULTING_ACTION_TYPE_FINISH -> {
                    val resultCode = ia.finishParams?.resultCode ?: ResultCode.RESULT_CODE_UNKNOWN
                    val finishParams = ia.finishParams
                    Log.d(TAG, "executeAction: id=$targetCid FINISH code=$resultCode")
                    ActionResult.Finish(resultCode, finishParams)
                }
                else -> {
                    Log.d(TAG, "executeAction: id=$targetCid unknown type=${ia.resultActionType}")
                    null
                }
            }
        }

        return null
    }

    fun getExecutionState(componentId: Long): FunctionalDataExecutionState {
        return executionStates[componentId]
            ?: FunctionalDataExecutionState.FUNCTIONAL_DATA_EXECUTION_STATE_UNKNOWN
    }

    fun setExecutionState(componentId: Long, state: FunctionalDataExecutionState) {
        executionStates[componentId] = state
    }

    /**
     * Get the current state of the AnimatedImage
     * 0=UNKNOWN, 1=NOT_STARTED, 2=RUNNING, 3=COMPLETED
     */
    fun getAnimatedImageState(componentId: Long): Int {
        return animatedImageStates[componentId] ?: 1 // default NOT_STARTED
    }

    fun reset() {
        conditionGraph.clear()
        resultingActionGraph.clear()
        components.clear()
        executionStates.clear()
        animatedImageStates.clear()
    }
}

sealed class ActionResult {
    object Submit : ActionResult()
    data class Finish(
        val resultCode: ResultCode,
        val finishParams: FinishActionParams? = null
    ) : ActionResult()
    data class StateChange(
        val componentId: Long,
        val newState: FunctionalDataExecutionState
    ) : ActionResult()
    data class AnimatedImageStateChange(
        val componentId: Long,
        val newState: Int  // 0=UNKNOWN, 1=NOT_STARTED, 2=RUNNING, 3=COMPLETED
    ) : ActionResult()
    data class EnablementChange(
        val componentId: Long,
        val enablementState: Int  // 1=ENABLED, 2=DISABLED
    ) : ActionResult()
    data class ConditionValueChange(
        val componentId: Long,
        val newConditionValue: Int
    ) : ActionResult()
    data class ValidationPassed(
        val componentId: Long
    ) : ActionResult()
}
