/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wallet.activity

import android.util.Log
import org.microg.vending.billing.proto.ComponentTreeNode
import org.microg.vending.billing.proto.ContainerNodeExtension
import org.microg.vending.billing.proto.ConditionalNodeExtension
import org.microg.vending.billing.proto.FullSheetNodeExtension
import org.microg.vending.billing.proto.LayoutModeProto
import org.microg.vending.billing.proto.ScrollNodeExtension

class ComponentTreeManager {

    companion object {
        private const val TAG = "ComponentTreeManager"

        private const val EXT_CONTAINER = 214299793
        private const val EXT_CONDITIONAL = 231420908
        private const val EXT_FULL_SHEET = 264434503
        private const val EXT_SCROLL = 229613734
        private const val EXT_TERMINAL = 265707483
        private const val EXT_EMPTY = 236049775
        private const val EXT_BUTTON = 232057537
        private const val EXT_TEXT_INPUT = 213678542
        private const val EXT_FORMATTED_TEXT = 213712846
        private const val EXT_SCROLL_VARIANT = 229613736

        private val LEAF_NODE_TYPES = setOf(
            EXT_TERMINAL, EXT_EMPTY, EXT_BUTTON,
            EXT_TEXT_INPUT, EXT_FORMATTED_TEXT, EXT_SCROLL_VARIANT
        )
    }

    private var currentTree: ComponentTreeNode? = null

    private var nodeMap = mutableMapOf<Long, ComponentTreeNode>()

    private var initialConditionValues = mutableMapOf<Long, Int>()

    fun initTree(tree: ComponentTreeNode?) {
        currentTree = tree
        nodeMap.clear()
        initialConditionValues.clear()
        if (tree != null) {
            flattenTree(tree, nodeMap, overwrite = true)
            extractInitialConditionValues(tree)
        }
        Log.d(TAG, "initTree: root nodeId=${tree?.nodeId}, nodeTypeId=${tree?.nodeTypeId}, " +
                "nodeMap=${nodeMap.size}, initCondVals=$initialConditionValues")
    }

    fun getInitialConditionValues(): Map<Long, Int> = initialConditionValues.toMap()

    private fun extractInitialConditionValues(node: ComponentTreeNode) {
        if (node.nodeTypeId == EXT_CONDITIONAL) {
            val condRef = node.conditionRef
            val condValue = node.conditionalExt?.conditionValue
            if (condRef != null && condValue != null) {
                initialConditionValues[condRef] = condValue
            }
        }
        for (child in getChildren(node)) {
            extractInitialConditionValues(child)
        }
    }

    fun mergeFragments(fragments: List<ComponentTreeNode>) {
        val root = currentTree ?: return
        if (fragments.isEmpty()) {
            Log.d(TAG, "mergeFragments: no fragments, skip")
            return
        }

        Log.d(TAG, "mergeFragments: ${fragments.size} fragments, nodeIds=${fragments.map { it.nodeId }}")

        for (fragment in fragments) {
            val nodeId = fragment.nodeId ?: continue
            if (nodeMap.containsKey(nodeId)) {
                nodeMap[nodeId] = fragment
                for (child in getChildren(fragment)) {
                    flattenTree(child, nodeMap, overwrite = false)
                }
                Log.d(TAG, "mergeFragments: replaced nodeId=$nodeId")
            } else {
                Log.w(TAG, "mergeFragments: nodeId=$nodeId not found in nodeMap, skip")
            }
        }

        Log.d(TAG, "mergeFragments: nodeMap=${nodeMap.size} nodes after merge")

        currentTree = rebuildTree(root, nodeMap)
        Log.d(TAG, "mergeFragments: rebuild complete")
    }

    fun getTree(): ComponentTreeNode? = currentTree

    // 1=RELATIVE(Box), 2=FLEX(Column)
    fun getLayoutModes(): Map<Long, LayoutModeProto> {
        val result = mutableMapOf<Long, LayoutModeProto>()
        for ((_, node) in nodeMap) {
            if (node.nodeTypeId != EXT_CONTAINER) continue
            val condRef = node.conditionRef ?: continue
            val layoutMode = node.containerExt?.layoutMode ?: LayoutModeProto.LAYOUT_MODE_FLEX
            result[condRef] = layoutMode
        }
        return result
    }

    fun reset() {
        currentTree = null
        nodeMap.clear()
        initialConditionValues.clear()
    }

    private fun flattenTree(node: ComponentTreeNode, map: MutableMap<Long, ComponentTreeNode>, overwrite: Boolean) {
        val nodeId = node.nodeId ?: return
        if (overwrite || !map.containsKey(nodeId)) {
            map[nodeId] = node
        }
        for (child in getChildren(node)) {
            flattenTree(child, map, overwrite)
        }
    }

    private fun getChildren(node: ComponentTreeNode): List<ComponentTreeNode> {
        return when (node.nodeTypeId) {
            EXT_CONTAINER -> {
                val ext = node.containerExt ?: return emptyList()
                val result = mutableListOf<ComponentTreeNode>()
                result.addAll(ext.children)
                ext.footer?.let { result.add(it) }
                result
            }
            EXT_CONDITIONAL -> {
                node.conditionalExt?.children ?: emptyList()
            }
            EXT_FULL_SHEET -> {
                listOfNotNull(node.fullSheetExt?.child)
            }
            EXT_SCROLL -> {
                listOfNotNull(node.scrollExt?.child)
            }
            else -> {
                if (node.nodeTypeId !in LEAF_NODE_TYPES) {
                    Log.w(TAG, "Unhandled tree nodeTypeId=${node.nodeTypeId}, nodeId=${node.nodeId}")
                }
                emptyList()
            }
        }
    }

    /**
     * Recursively rebuild the component tree from the root.
     * If a node or any of its descendants has been replaced in the nodeMap,
     * the child node references in the extension data need to be rebuilt.
     */
    private fun rebuildTree(
        originalNode: ComponentTreeNode,
        nodeMap: Map<Long, ComponentTreeNode>
    ): ComponentTreeNode {
        val nodeId = originalNode.nodeId ?: return originalNode
        val currentNode = nodeMap[nodeId] ?: return originalNode

        // Get the current child nodes and recursively rebuild each one
        val currentChildren = getChildren(currentNode)
        if (currentChildren.isEmpty()) {
            return currentNode // Leaf node — return directly
        }

        val rebuiltChildren = currentChildren.map { child ->
            val childId = child.nodeId
            if (childId != null && nodeMap.containsKey(childId)) {
                rebuildTree(child, nodeMap)
            } else {
                child
            }
        }

        // Rebuild child node references by nodeTypeId
        return setChildren(currentNode, rebuiltChildren)
    }

    private fun setChildren(
        node: ComponentTreeNode,
        children: List<ComponentTreeNode>
    ): ComponentTreeNode {
        return when (node.nodeTypeId) {
            EXT_CONTAINER -> {
                val ext = node.containerExt ?: ContainerNodeExtension()
                // Keep the original footer if it was the last item in the children list and a footer existed originally
                val originalFooter = node.containerExt?.footer
                val newChildren: List<ComponentTreeNode>
                val newFooter: ComponentTreeNode?
                if (originalFooter != null && children.isNotEmpty()) {
                    // The last one may be a footer
                    val footerNodeId = originalFooter.nodeId
                    if (children.last().nodeId == footerNodeId) {
                        newChildren = children.dropLast(1)
                        newFooter = children.last()
                    } else {
                        newChildren = children
                        newFooter = null
                    }
                } else {
                    newChildren = children
                    newFooter = null
                }
                node.copy(
                    containerExt = ext.copy(
                        children = newChildren,
                        footer = newFooter
                    )
                )
            }
            EXT_CONDITIONAL -> {
                val ext = node.conditionalExt ?: ConditionalNodeExtension()
                node.copy(
                    conditionalExt = ext.copy(children = children)
                )
            }
            EXT_FULL_SHEET -> {
                node.copy(
                    fullSheetExt = (node.fullSheetExt ?: FullSheetNodeExtension()).copy(
                        child = children.firstOrNull()
                    )
                )
            }
            EXT_SCROLL -> {
                node.copy(
                    scrollExt = (node.scrollExt ?: ScrollNodeExtension()).copy(
                        child = children.firstOrNull()
                    )
                )
            }
            else -> node
        }
    }
}
