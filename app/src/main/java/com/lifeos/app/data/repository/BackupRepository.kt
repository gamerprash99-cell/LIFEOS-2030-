package com.lifeos.app.data.repository

import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.data.db.entities.TaskEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * "Your life. Your data." — complete local JSON export/import. Files stay in
 * app-private storage and are only shared/saved when the user explicitly asks.
 */
@Serializable
data class LifeOSBackup(
    val exportedAtEpochMillis: Long,
    val appVersion: String,
    val notes: List<NoteEntity>,
    val tasks: List<TaskEntity>,
    val habits: List<HabitEntity>,
    val habitCompletions: List<HabitCompletionEntity>,
    val expenses: List<ExpenseEntity>,
    val diaryEntries: List<DiaryEntity>,
    val captures: List<CaptureEntity>
)

class BackupRepository(
    private val noteRepo: NoteRepository,
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository,
    private val expenseRepo: ExpenseRepository,
    private val diaryRepo: DiaryRepository,
    private val captureRepo: CaptureRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun buildBackup(appVersion: String): LifeOSBackup = LifeOSBackup(
        exportedAtEpochMillis = System.currentTimeMillis(),
        appVersion = appVersion,
        notes = noteRepo.getAllForBackup(),
        tasks = taskRepo.getAllForBackup(),
        habits = habitRepo.getAllForBackup(),
        habitCompletions = habitRepo.getAllCompletionsForBackup(),
        expenses = expenseRepo.getAllForBackup(),
        diaryEntries = diaryRepo.getAllForBackup(),
        captures = captureRepo.getAllForBackup()
    )

    suspend fun exportToFile(directory: File, appVersion: String): File {
        val backup = buildBackup(appVersion)
        val text = json.encodeToString(backup)
        val file = File(directory, "lifeos-backup-${backup.exportedAtEpochMillis}.json")
        file.writeText(text)
        return file
    }

    suspend fun importFromFile(file: File) {
        val backup: LifeOSBackup = json.decodeFromString(file.readText())
        restore(backup)
    }

    suspend fun restore(backup: LifeOSBackup) {
        noteRepo.restoreFromBackup(backup.notes)
        taskRepo.restoreFromBackup(backup.tasks)
        habitRepo.restoreFromBackup(backup.habits, backup.habitCompletions)
        expenseRepo.restoreFromBackup(backup.expenses)
        diaryRepo.restoreFromBackup(backup.diaryEntries)
        captureRepo.restoreFromBackup(backup.captures)
    }
}
