package com.vaibhav.taskify.data.repo

import com.vaibhav.taskify.data.local.dataSource.TaskDataSource
import com.vaibhav.taskify.data.models.entity.TaskEntity
import com.vaibhav.taskify.data.models.mappper.TaskMapper
import com.vaibhav.taskify.data.models.remote.TaskDTO
import com.vaibhav.taskify.data.remote.dataSource.HarperDbTaskDataSource
import com.vaibhav.taskify.util.Resource
import com.vaibhav.taskify.util.TaskState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class TaskRepo @Inject constructor(
    private val taskDataSource: TaskDataSource,
    private val harperDbTaskDataSource: HarperDbTaskDataSource,
    private val taskMapper: TaskMapper
) {

    private fun getTodaysTime(): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        Timber.d(cal.timeInMillis.toString())
        return cal.timeInMillis
    }


    fun getAllPausedTasks() = taskDataSource.getAllTasksOfToday(
        state = TaskState.PAUSED.name, todaysTime = getTodaysTime()
    )

    fun getAllCompletedTasksOfToday() = taskDataSource.getAllTasksOfToday(
        state = TaskState.COMPLETED.name, todaysTime = getTodaysTime()
    )

    fun getAllUpComingTasksOfToday() = taskDataSource.getAllTasksOfToday(
        state = TaskState.NOT_STARTED.name, todaysTime = getTodaysTime()
    )

    fun getRunningTask() = taskDataSource.getAllTasksOfToday(
        state = TaskState.RUNNING.name,
        todaysTime = getTodaysTime()
    )

    suspend fun fetchAllTasks(email: String): Resource<Unit> = withContext(Dispatchers.IO) {
        val resource = harperDbTaskDataSource.getAllTasksOfUser(email)
        Timber.d(resource.data.toString())
        if (resource is Resource.Success) {
            saveAllNewDataInDb(resource.data!!)
            Resource.Success(message = "Tasks fetched successfully")
        } else Resource.Error(message = resource.message)
    }

    suspend fun addNewTask(taskEntity: TaskEntity): Resource<Unit> = withContext(Dispatchers.IO) {
        val taskDAO = taskMapper.toNetwork(taskEntity)
        val resource = harperDbTaskDataSource.insertTask(taskDAO)
        if (resource is Resource.Success) {
            insertTasksIntoDb(listOf(taskEntity))
            Resource.Success(message = "Tasks saved successfully")
        } else Resource.Error(message = resource.message)
    }

    suspend fun updateTask(taskEntity: TaskEntity): Resource<Unit> = withContext(Dispatchers.IO) {
        val taskDAO = taskMapper.toNetwork(taskEntity)
        val resource = harperDbTaskDataSource.updateTask(taskDAO)
        if (resource is Resource.Success) {
            updateTaskInDB(taskEntity)
            Resource.Success(message = "Tasks updated successfully")
        } else Resource.Error(message = resource.message)
    }

    suspend fun deleteTask(taskEntity: TaskEntity): Resource<Unit> = withContext(Dispatchers.IO) {
        val resource = harperDbTaskDataSource.deleteTask(taskEntity.task_id)
        if (resource is Resource.Success) {
            deleteTaskFromDb(taskEntity)
            Resource.Success(message = "Tasks deleted successfully")
        } else Resource.Error(message = resource.message)
    }

    private suspend fun saveAllNewDataInDb(taskDTO: List<TaskDTO>) {
        deleteAllTasks()
        val taskEntities = taskMapper.toDomainList(taskDTO)
        insertTasksIntoDb(taskEntities)
    }

    private suspend fun insertTasksIntoDb(taskEntities: List<TaskEntity>) {
        taskDataSource.insertTask(taskEntities)
    }

    private suspend fun deleteAllTasks() = taskDataSource.deleteAllTasks()

    private suspend fun updateTaskInDB(taskEntity: TaskEntity) =
        taskDataSource.updateTask(taskEntity)

    private suspend fun deleteTaskFromDb(taskEntity: TaskEntity) =
        taskDataSource.deleteTask(taskEntity)

}