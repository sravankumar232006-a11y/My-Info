package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. Entities ---

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val rollNumber: String,
    val name: String,
    val department: String,
    val branch: String,
    val semester: String,
    val cgpa: Double,
    val attendancePercentage: Double,
    val email: String,
    val phone: String,
    val address: String
)

@Entity(tableName = "attendance_logs")
data class AttendanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectCode: String,
    val subjectName: String,
    val date: String, // YYYY-MM-DD
    val status: String, // "Present", "Absent", "Late"
    val remarks: String
)

@Entity(tableName = "helpdesk_tickets")
data class HelpdeskTicketEntity(
    @PrimaryKey val id: String,
    val category: String, // "Academic", "Hostel", "Transport", "Accounts", "Technical"
    val title: String,
    val description: String,
    val status: String, // "Open", "In Progress", "Resolved"
    val timestamp: Long,
    val messagesJson: String // Serialized JSON chat history
)

@Entity(tableName = "academic_subjects")
data class SubjectEntity(
    @PrimaryKey val code: String,
    val name: String,
    val credits: Int,
    val attendanceCount: Int,
    val totalClasses: Int,
    val internalMarks: Int, // Out of 50
    val grade: String, // "A+", "A", "B", etc.
    val syllabus: String,
    val facultyName: String,
    val facultyEmail: String,
    val classroom: String
)

@Entity(tableName = "campus_circulars")
data class CircularEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String, // "Notice", "Placement", "Event", "Circular"
    val timestamp: Long,
    val publisher: String
)

// --- 2. DAOs ---

@Dao
interface CampusDao {
    // Profile
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    // Attendance Logs
    @Query("SELECT * FROM attendance_logs ORDER BY date DESC")
    fun getAllAttendanceLogsFlow(): Flow<List<AttendanceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceLog(log: AttendanceLogEntity)

    // Helpdesk Tickets
    @Query("SELECT * FROM helpdesk_tickets ORDER BY timestamp DESC")
    fun getAllTicketsFlow(): Flow<List<HelpdeskTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: HelpdeskTicketEntity)

    @Query("SELECT * FROM helpdesk_tickets WHERE id = :id")
    suspend fun getTicketById(id: String): HelpdeskTicketEntity?

    // Subjects
    @Query("SELECT * FROM academic_subjects")
    fun getAllSubjectsFlow(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    // Circulars
    @Query("SELECT * FROM campus_circulars ORDER BY timestamp DESC")
    fun getAllCircularsFlow(): Flow<List<CircularEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCirculars(circulars: List<CircularEntity>)
}

// --- 3. Database ---

@Database(
    entities = [
        UserProfileEntity::class,
        AttendanceLogEntity::class,
        HelpdeskTicketEntity::class,
        SubjectEntity::class,
        CircularEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun campusDao(): CampusDao

    companion object {
        @Volatile
        private var INSTANCE: CampusDatabase? = null

        fun getDatabase(context: Context): CampusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CampusDatabase::class.java,
                    "campus_erp_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- 4. Repository ---

class CampusRepository(private val campusDao: CampusDao) {
    val userProfile: Flow<UserProfileEntity?> = campusDao.getUserProfileFlow()
    val attendanceLogs: Flow<List<AttendanceLogEntity>> = campusDao.getAllAttendanceLogsFlow()
    val helpdeskTickets: Flow<List<HelpdeskTicketEntity>> = campusDao.getAllTicketsFlow()
    val academicSubjects: Flow<List<SubjectEntity>> = campusDao.getAllSubjectsFlow()
    val campusCirculars: Flow<List<CircularEntity>> = campusDao.getAllCircularsFlow()

    suspend fun getProfileDirect(): UserProfileEntity? = campusDao.getUserProfile()

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        campusDao.insertUserProfile(profile)
    }

    suspend fun addAttendanceLog(log: AttendanceLogEntity) {
        campusDao.insertAttendanceLog(log)
    }

    suspend fun addOrUpdateTicket(ticket: HelpdeskTicketEntity) {
        campusDao.insertTicket(ticket)
    }

    suspend fun getTicketById(id: String): HelpdeskTicketEntity? {
        return campusDao.getTicketById(id)
    }

    suspend fun updateSubject(subject: SubjectEntity) {
        campusDao.updateSubject(subject)
    }

    suspend fun seedDatabase(
        profile: UserProfileEntity,
        subjects: List<SubjectEntity>,
        circulars: List<CircularEntity>
    ) {
        campusDao.insertUserProfile(profile)
        campusDao.insertSubjects(subjects)
        campusDao.insertCirculars(circulars)
    }
}
