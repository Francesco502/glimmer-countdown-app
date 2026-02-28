package com.example.timeapk.ui.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.timeapk.R

/** 已知节点返回 string res id，自定义节点返回 null（调用方用 milestone_days_format 格式化）. */
fun getMilestoneLabelRes(value: Long): Int? = when (value) {
    7L -> R.string.milestone_7
    30L -> R.string.milestone_30
    100L -> R.string.milestone_100
    365L -> R.string.milestone_365
    520L -> R.string.milestone_520
    1000L -> R.string.milestone_1000
    else -> null
}

@Composable
fun milestoneLabel(value: Long): String {
    val resId = getMilestoneLabelRes(value)
    return if (resId != null) stringResource(resId)
    else stringResource(R.string.milestone_days_format, value)
}

/** 非 Composable 场景下获取节点描述（如 Worker、后台调度）. */
fun getMilestoneLabel(context: Context, value: Long): String {
    val resId = getMilestoneLabelRes(value)
    return if (resId != null) context.getString(resId)
    else context.getString(R.string.milestone_days_format, value)
}
