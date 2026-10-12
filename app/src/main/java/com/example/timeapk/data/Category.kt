package com.example.timeapk.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** 内置分类 id；内置分类不入表，名称走字符串资源。 */
val BUILT_IN_CATEGORY_IDS: List<String> = listOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY, CATEGORY_OTHER)

const val CUSTOM_CATEGORY_ID_PREFIX = "custom_"
const val CATEGORY_NAME_MAX_LENGTH = 12

fun isBuiltInCategory(id: String): Boolean = id in BUILT_IN_CATEGORY_IDS

fun isCustomCategoryId(id: String): Boolean =
    id.startsWith(CUSTOM_CATEGORY_ID_PREFIX) && id.length > CUSTOM_CATEGORY_ID_PREFIX.length

fun newCustomCategoryId(): String = CUSTOM_CATEGORY_ID_PREFIX + UUID.randomUUID().toString().replace("-", "")

/** 用户自定义分类。事件通过 [Event.category] 保存分类 id。 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

fun normalizeCategoryName(name: String): String =
    name.trim().replace(Regex("\\s+"), " ").take(CATEGORY_NAME_MAX_LENGTH)

/** 把未知或已删除的分类回落为“其他”，内置与已存在的自定义分类保持原样。 */
fun resolveCategoryId(id: String, customCategoryIds: Set<String>): String = when {
    isBuiltInCategory(id) -> id
    id in customCategoryIds -> id
    else -> CATEGORY_OTHER
}

@Dao
abstract class CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, createdAt ASC")
    abstract fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, createdAt ASC")
    abstract suspend fun getAllSnapshot(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(categories: List<Category>)

    @Update
    abstract suspend fun update(category: Category)

    @Update
    abstract suspend fun updateAll(categories: List<Category>)

    @Query("DELETE FROM categories WHERE id = :id")
    abstract suspend fun deleteById(id: String)

    @Query("UPDATE events SET category = :replacement WHERE category = :id")
    abstract suspend fun reassignEvents(id: String, replacement: String): Int

    @Query("SELECT COUNT(*) FROM events WHERE category = :id")
    abstract suspend fun countEvents(id: String): Int

    /** 删除分类并把其事件改为“其他”，同一事务内完成。 */
    @Transaction
    open suspend fun deleteAndReassign(id: String): Int {
        val moved = reassignEvents(id, CATEGORY_OTHER)
        deleteById(id)
        return moved
    }
}

class CategoryRepository(private val dao: CategoryDao) {
    fun getAll(): Flow<List<Category>> = dao.getAll()

    suspend fun getAllSnapshot(): List<Category> = dao.getAllSnapshot()

    suspend fun add(name: String, colorHex: String?): Category? {
        val normalized = normalizeCategoryName(name)
        if (normalized.isEmpty()) return null
        val existing = dao.getAllSnapshot()
        existing.firstOrNull { it.name.equals(normalized, ignoreCase = true) }?.let { return it }
        val category = Category(
            id = newCustomCategoryId(),
            name = normalized,
            colorHex = colorHex,
            sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        )
        dao.insert(category)
        return category
    }

    suspend fun update(category: Category) {
        val normalized = normalizeCategoryName(category.name)
        if (normalized.isEmpty()) return
        dao.update(category.copy(name = normalized))
    }

    suspend fun reorder(orderedIds: List<String>) {
        val byId = dao.getAllSnapshot().associateBy { it.id }
        val updated = orderedIds.mapIndexedNotNull { index, id -> byId[id]?.copy(sortOrder = index) }
        if (updated.isNotEmpty()) dao.updateAll(updated)
    }

    suspend fun countEvents(id: String): Int = dao.countEvents(id)

    suspend fun delete(id: String): Int {
        if (!isCustomCategoryId(id)) return 0
        return dao.deleteAndReassign(id)
    }

    suspend fun insertAll(categories: List<Category>) {
        if (categories.isNotEmpty()) dao.insertAll(categories)
    }
}
