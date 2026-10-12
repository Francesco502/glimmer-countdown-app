package com.example.timeapk.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.timeapk.R
import com.example.timeapk.data.BUILT_IN_CATEGORY_IDS
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Category
import com.example.timeapk.ui.theme.SongPalette
import com.example.timeapk.ui.utils.parseEventColorOrFallback

/** 当前用户的自定义分类，由 TimeApp 根部提供。 */
val LocalCustomCategories = staticCompositionLocalOf<List<Category>> { emptyList() }

data class CategoryOption(
    val id: String,
    val label: String,
    val color: Color,
    val builtIn: Boolean
)

/** 自定义分类可选的颜色，取自宋韵色板。 */
val CategoryColorChoices: List<String> = listOf(
    "#AF4E31", // 朱
    "#457080", // 青
    "#5B8E79", // 碧
    "#AC8F62", // 金
    "#86351C", // 赭
    "#7A6A9A", // 藕荷
    "#B5677D", // 胭脂
    "#4F5D73"  // 黛
)

fun builtInCategoryColor(id: String): Color = when (id) {
    CATEGORY_BIRTHDAY -> SongPalette.Seal
    CATEGORY_ANNIVERSARY -> SongPalette.Celadon
    else -> SongPalette.Jade
}

fun builtInCategoryLabelRes(id: String): Int = when (id) {
    CATEGORY_BIRTHDAY -> R.string.category_birthday
    CATEGORY_ANNIVERSARY -> R.string.category_anniversary
    else -> R.string.category_other
}

fun categoryLabel(context: Context, id: String, customCategories: List<Category>): String {
    if (id in BUILT_IN_CATEGORY_IDS) return context.getString(builtInCategoryLabelRes(id))
    return customCategories.firstOrNull { it.id == id }?.name
        ?: context.getString(R.string.category_other)
}

fun categoryColor(id: String, customCategories: List<Category>): Color {
    if (id in BUILT_IN_CATEGORY_IDS) return builtInCategoryColor(id)
    val custom = customCategories.firstOrNull { it.id == id } ?: return builtInCategoryColor(CATEGORY_OTHER)
    return parseEventColorOrFallback(custom.colorHex, builtInCategoryColor(CATEGORY_OTHER))
}

fun categoryOptions(context: Context, customCategories: List<Category>): List<CategoryOption> =
    BUILT_IN_CATEGORY_IDS.map { id ->
        CategoryOption(id, context.getString(builtInCategoryLabelRes(id)), builtInCategoryColor(id), builtIn = true)
    } + customCategories.map { category ->
        CategoryOption(
            id = category.id,
            label = category.name,
            color = categoryColor(category.id, customCategories),
            builtIn = false
        )
    }

@Composable
@ReadOnlyComposable
fun categoryLabel(id: String): String =
    categoryLabel(LocalContext.current, id, LocalCustomCategories.current)

@Composable
@ReadOnlyComposable
fun categoryColor(id: String): Color = categoryColor(id, LocalCustomCategories.current)

@Composable
@ReadOnlyComposable
fun categoryOptions(): List<CategoryOption> =
    categoryOptions(LocalContext.current, LocalCustomCategories.current)
