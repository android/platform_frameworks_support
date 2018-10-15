/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.benchmark

import androidx.benchmark.BenchmarkRule
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.InvalidationTracker
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.InstrumentationRegistry
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Arrays

@LargeTest
@RunWith(Parameterized::class)
class InvalidationTrackerBenchmark(private val sampleSize: Int) {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    val context = InstrumentationRegistry.getTargetContext()

    @Before
    fun setup() {
        for (postfix in arrayOf("", "-wal", "-shm")) {
            val dbFile = context.getDatabasePath(DB_NAME + postfix)
            if (dbFile.exists()) {
                assertTrue(dbFile.delete())
            }
        }
    }

    @Test
    fun largeTransaction() {
        val db = Room.databaseBuilder(context, TestDatabase::class.java, DB_NAME)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()

        val observer = object : InvalidationTracker.Observer("user") {
            override fun onInvalidated(tables: MutableSet<String>) {}
        }
        db.invalidationTracker.addObserver(observer)

        benchmarkRule.state.pauseTiming()
        val users = List(sampleSize) { User(it, "name$it") }
        benchmarkRule.state.resumeTiming()

        while (benchmarkRule.state.keepRunning()) {
            // Insert the sample size
            db.runInTransaction {
                for (user in users) {
                    db.getUserDao().insert(user)
                }
            }

            // Delete sample size (causing a large transaction)
            assertEquals(db.getUserDao().deleteAll(), sampleSize)
        }

        db.close()
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "sampleSize={0}")
        fun data(): List<Int> {
            return Arrays.asList(100, 1000, 5000, 10000)
        }

        private const val DB_NAME = "invalidation-benchmark-test"
    }
}

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class TestDatabase : RoomDatabase() {
    abstract fun getUserDao(): UserDao
}

@Entity
data class User(@PrimaryKey val id: Int, val name: String)

@Dao
interface UserDao {
    @Insert
    fun insert(user: User)

    @Query("DELETE FROM User")
    fun deleteAll(): Int
}