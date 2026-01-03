package com.cognaque.sequence.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TaskDao_Impl implements TaskDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Task> __insertionAdapterOfTask;

  private final EntityInsertionAdapter<DailyChore> __insertionAdapterOfDailyChore;

  private final EntityDeletionOrUpdateAdapter<Task> __deletionAdapterOfTask;

  private final EntityDeletionOrUpdateAdapter<DailyChore> __deletionAdapterOfDailyChore;

  private final EntityDeletionOrUpdateAdapter<Task> __updateAdapterOfTask;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllTasks;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllDailyChores;

  public TaskDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTask = new EntityInsertionAdapter<Task>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tasks` (`id`,`rawText`,`normalizedSignature`,`immediate`,`longTerm`,`proximity`,`accumulation`,`effort`,`isDone`,`isAiGenerated`,`needsClarification`,`needsReEvaluation`,`isManuallyPromoted`,`isDailyChore`,`entryCount`,`creationTimestamp`,`notes`,`parentId`,`orderIndex`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Task entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getRawText());
        statement.bindString(3, entity.getNormalizedSignature());
        statement.bindDouble(4, entity.getImmediate());
        statement.bindDouble(5, entity.getLongTerm());
        statement.bindDouble(6, entity.getProximity());
        statement.bindDouble(7, entity.getAccumulation());
        statement.bindDouble(8, entity.getEffort());
        final int _tmp = entity.isDone() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.isAiGenerated() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.getNeedsClarification() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        final int _tmp_3 = entity.getNeedsReEvaluation() ? 1 : 0;
        statement.bindLong(12, _tmp_3);
        final int _tmp_4 = entity.isManuallyPromoted() ? 1 : 0;
        statement.bindLong(13, _tmp_4);
        final int _tmp_5 = entity.isDailyChore() ? 1 : 0;
        statement.bindLong(14, _tmp_5);
        statement.bindLong(15, entity.getEntryCount());
        statement.bindLong(16, entity.getCreationTimestamp());
        statement.bindString(17, entity.getNotes());
        if (entity.getParentId() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getParentId());
        }
        statement.bindLong(19, entity.getOrderIndex());
      }
    };
    this.__insertionAdapterOfDailyChore = new EntityInsertionAdapter<DailyChore>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_chores` (`id`,`text`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyChore entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getText());
      }
    };
    this.__deletionAdapterOfTask = new EntityDeletionOrUpdateAdapter<Task>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `tasks` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Task entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__deletionAdapterOfDailyChore = new EntityDeletionOrUpdateAdapter<DailyChore>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `daily_chores` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyChore entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfTask = new EntityDeletionOrUpdateAdapter<Task>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `tasks` SET `id` = ?,`rawText` = ?,`normalizedSignature` = ?,`immediate` = ?,`longTerm` = ?,`proximity` = ?,`accumulation` = ?,`effort` = ?,`isDone` = ?,`isAiGenerated` = ?,`needsClarification` = ?,`needsReEvaluation` = ?,`isManuallyPromoted` = ?,`isDailyChore` = ?,`entryCount` = ?,`creationTimestamp` = ?,`notes` = ?,`parentId` = ?,`orderIndex` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Task entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getRawText());
        statement.bindString(3, entity.getNormalizedSignature());
        statement.bindDouble(4, entity.getImmediate());
        statement.bindDouble(5, entity.getLongTerm());
        statement.bindDouble(6, entity.getProximity());
        statement.bindDouble(7, entity.getAccumulation());
        statement.bindDouble(8, entity.getEffort());
        final int _tmp = entity.isDone() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.isAiGenerated() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.getNeedsClarification() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        final int _tmp_3 = entity.getNeedsReEvaluation() ? 1 : 0;
        statement.bindLong(12, _tmp_3);
        final int _tmp_4 = entity.isManuallyPromoted() ? 1 : 0;
        statement.bindLong(13, _tmp_4);
        final int _tmp_5 = entity.isDailyChore() ? 1 : 0;
        statement.bindLong(14, _tmp_5);
        statement.bindLong(15, entity.getEntryCount());
        statement.bindLong(16, entity.getCreationTimestamp());
        statement.bindString(17, entity.getNotes());
        if (entity.getParentId() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getParentId());
        }
        statement.bindLong(19, entity.getOrderIndex());
        statement.bindString(20, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAllTasks = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tasks";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllDailyChores = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM daily_chores";
        return _query;
      }
    };
  }

  @Override
  public Object insertTask(final Task task, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTask.insert(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertDailyChoreTemplate(final DailyChore chore,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailyChore.insert(chore);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTask(final Task task, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTask.handle(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDailyChoreTemplate(final DailyChore chore,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDailyChore.handle(chore);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTask(final Task task, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTask.handle(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTasks(final List<Task> tasks, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTask.handleMultiple(tasks);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllTasks(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllTasks.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllTasks.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllDailyChores(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllDailyChores.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllDailyChores.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Task>> getActiveTasksFlow() {
    final String _sql = "SELECT `tasks`.`id` AS `id`, `tasks`.`rawText` AS `rawText`, `tasks`.`normalizedSignature` AS `normalizedSignature`, `tasks`.`immediate` AS `immediate`, `tasks`.`longTerm` AS `longTerm`, `tasks`.`proximity` AS `proximity`, `tasks`.`accumulation` AS `accumulation`, `tasks`.`effort` AS `effort`, `tasks`.`isDone` AS `isDone`, `tasks`.`isAiGenerated` AS `isAiGenerated`, `tasks`.`needsClarification` AS `needsClarification`, `tasks`.`needsReEvaluation` AS `needsReEvaluation`, `tasks`.`isManuallyPromoted` AS `isManuallyPromoted`, `tasks`.`isDailyChore` AS `isDailyChore`, `tasks`.`entryCount` AS `entryCount`, `tasks`.`creationTimestamp` AS `creationTimestamp`, `tasks`.`notes` AS `notes`, `tasks`.`parentId` AS `parentId`, `tasks`.`orderIndex` AS `orderIndex` FROM tasks WHERE isDone = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<Task>>() {
      @Override
      @NonNull
      public List<Task> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfRawText = 1;
          final int _cursorIndexOfNormalizedSignature = 2;
          final int _cursorIndexOfImmediate = 3;
          final int _cursorIndexOfLongTerm = 4;
          final int _cursorIndexOfProximity = 5;
          final int _cursorIndexOfAccumulation = 6;
          final int _cursorIndexOfEffort = 7;
          final int _cursorIndexOfIsDone = 8;
          final int _cursorIndexOfIsAiGenerated = 9;
          final int _cursorIndexOfNeedsClarification = 10;
          final int _cursorIndexOfNeedsReEvaluation = 11;
          final int _cursorIndexOfIsManuallyPromoted = 12;
          final int _cursorIndexOfIsDailyChore = 13;
          final int _cursorIndexOfEntryCount = 14;
          final int _cursorIndexOfCreationTimestamp = 15;
          final int _cursorIndexOfNotes = 16;
          final int _cursorIndexOfParentId = 17;
          final int _cursorIndexOfOrderIndex = 18;
          final List<Task> _result = new ArrayList<Task>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Task _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRawText;
            _tmpRawText = _cursor.getString(_cursorIndexOfRawText);
            final String _tmpNormalizedSignature;
            _tmpNormalizedSignature = _cursor.getString(_cursorIndexOfNormalizedSignature);
            final float _tmpImmediate;
            _tmpImmediate = _cursor.getFloat(_cursorIndexOfImmediate);
            final float _tmpLongTerm;
            _tmpLongTerm = _cursor.getFloat(_cursorIndexOfLongTerm);
            final float _tmpProximity;
            _tmpProximity = _cursor.getFloat(_cursorIndexOfProximity);
            final float _tmpAccumulation;
            _tmpAccumulation = _cursor.getFloat(_cursorIndexOfAccumulation);
            final float _tmpEffort;
            _tmpEffort = _cursor.getFloat(_cursorIndexOfEffort);
            final boolean _tmpIsDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp != 0;
            final boolean _tmpIsAiGenerated;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsAiGenerated);
            _tmpIsAiGenerated = _tmp_1 != 0;
            final boolean _tmpNeedsClarification;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNeedsClarification);
            _tmpNeedsClarification = _tmp_2 != 0;
            final boolean _tmpNeedsReEvaluation;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfNeedsReEvaluation);
            _tmpNeedsReEvaluation = _tmp_3 != 0;
            final boolean _tmpIsManuallyPromoted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsManuallyPromoted);
            _tmpIsManuallyPromoted = _tmp_4 != 0;
            final boolean _tmpIsDailyChore;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsDailyChore);
            _tmpIsDailyChore = _tmp_5 != 0;
            final int _tmpEntryCount;
            _tmpEntryCount = _cursor.getInt(_cursorIndexOfEntryCount);
            final long _tmpCreationTimestamp;
            _tmpCreationTimestamp = _cursor.getLong(_cursorIndexOfCreationTimestamp);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpParentId;
            if (_cursor.isNull(_cursorIndexOfParentId)) {
              _tmpParentId = null;
            } else {
              _tmpParentId = _cursor.getString(_cursorIndexOfParentId);
            }
            final long _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getLong(_cursorIndexOfOrderIndex);
            _item = new Task(_tmpId,_tmpRawText,_tmpNormalizedSignature,_tmpImmediate,_tmpLongTerm,_tmpProximity,_tmpAccumulation,_tmpEffort,_tmpIsDone,_tmpIsAiGenerated,_tmpNeedsClarification,_tmpNeedsReEvaluation,_tmpIsManuallyPromoted,_tmpIsDailyChore,_tmpEntryCount,_tmpCreationTimestamp,_tmpNotes,_tmpParentId,_tmpOrderIndex);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllTasksOneShot(final Continuation<? super List<Task>> $completion) {
    final String _sql = "SELECT `tasks`.`id` AS `id`, `tasks`.`rawText` AS `rawText`, `tasks`.`normalizedSignature` AS `normalizedSignature`, `tasks`.`immediate` AS `immediate`, `tasks`.`longTerm` AS `longTerm`, `tasks`.`proximity` AS `proximity`, `tasks`.`accumulation` AS `accumulation`, `tasks`.`effort` AS `effort`, `tasks`.`isDone` AS `isDone`, `tasks`.`isAiGenerated` AS `isAiGenerated`, `tasks`.`needsClarification` AS `needsClarification`, `tasks`.`needsReEvaluation` AS `needsReEvaluation`, `tasks`.`isManuallyPromoted` AS `isManuallyPromoted`, `tasks`.`isDailyChore` AS `isDailyChore`, `tasks`.`entryCount` AS `entryCount`, `tasks`.`creationTimestamp` AS `creationTimestamp`, `tasks`.`notes` AS `notes`, `tasks`.`parentId` AS `parentId`, `tasks`.`orderIndex` AS `orderIndex` FROM tasks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Task>>() {
      @Override
      @NonNull
      public List<Task> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfRawText = 1;
          final int _cursorIndexOfNormalizedSignature = 2;
          final int _cursorIndexOfImmediate = 3;
          final int _cursorIndexOfLongTerm = 4;
          final int _cursorIndexOfProximity = 5;
          final int _cursorIndexOfAccumulation = 6;
          final int _cursorIndexOfEffort = 7;
          final int _cursorIndexOfIsDone = 8;
          final int _cursorIndexOfIsAiGenerated = 9;
          final int _cursorIndexOfNeedsClarification = 10;
          final int _cursorIndexOfNeedsReEvaluation = 11;
          final int _cursorIndexOfIsManuallyPromoted = 12;
          final int _cursorIndexOfIsDailyChore = 13;
          final int _cursorIndexOfEntryCount = 14;
          final int _cursorIndexOfCreationTimestamp = 15;
          final int _cursorIndexOfNotes = 16;
          final int _cursorIndexOfParentId = 17;
          final int _cursorIndexOfOrderIndex = 18;
          final List<Task> _result = new ArrayList<Task>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Task _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRawText;
            _tmpRawText = _cursor.getString(_cursorIndexOfRawText);
            final String _tmpNormalizedSignature;
            _tmpNormalizedSignature = _cursor.getString(_cursorIndexOfNormalizedSignature);
            final float _tmpImmediate;
            _tmpImmediate = _cursor.getFloat(_cursorIndexOfImmediate);
            final float _tmpLongTerm;
            _tmpLongTerm = _cursor.getFloat(_cursorIndexOfLongTerm);
            final float _tmpProximity;
            _tmpProximity = _cursor.getFloat(_cursorIndexOfProximity);
            final float _tmpAccumulation;
            _tmpAccumulation = _cursor.getFloat(_cursorIndexOfAccumulation);
            final float _tmpEffort;
            _tmpEffort = _cursor.getFloat(_cursorIndexOfEffort);
            final boolean _tmpIsDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp != 0;
            final boolean _tmpIsAiGenerated;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsAiGenerated);
            _tmpIsAiGenerated = _tmp_1 != 0;
            final boolean _tmpNeedsClarification;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNeedsClarification);
            _tmpNeedsClarification = _tmp_2 != 0;
            final boolean _tmpNeedsReEvaluation;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfNeedsReEvaluation);
            _tmpNeedsReEvaluation = _tmp_3 != 0;
            final boolean _tmpIsManuallyPromoted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsManuallyPromoted);
            _tmpIsManuallyPromoted = _tmp_4 != 0;
            final boolean _tmpIsDailyChore;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsDailyChore);
            _tmpIsDailyChore = _tmp_5 != 0;
            final int _tmpEntryCount;
            _tmpEntryCount = _cursor.getInt(_cursorIndexOfEntryCount);
            final long _tmpCreationTimestamp;
            _tmpCreationTimestamp = _cursor.getLong(_cursorIndexOfCreationTimestamp);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpParentId;
            if (_cursor.isNull(_cursorIndexOfParentId)) {
              _tmpParentId = null;
            } else {
              _tmpParentId = _cursor.getString(_cursorIndexOfParentId);
            }
            final long _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getLong(_cursorIndexOfOrderIndex);
            _item = new Task(_tmpId,_tmpRawText,_tmpNormalizedSignature,_tmpImmediate,_tmpLongTerm,_tmpProximity,_tmpAccumulation,_tmpEffort,_tmpIsDone,_tmpIsAiGenerated,_tmpNeedsClarification,_tmpNeedsReEvaluation,_tmpIsManuallyPromoted,_tmpIsDailyChore,_tmpEntryCount,_tmpCreationTimestamp,_tmpNotes,_tmpParentId,_tmpOrderIndex);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTaskById(final String id, final Continuation<? super Task> $completion) {
    final String _sql = "SELECT * FROM tasks WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Task>() {
      @Override
      @Nullable
      public Task call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRawText = CursorUtil.getColumnIndexOrThrow(_cursor, "rawText");
          final int _cursorIndexOfNormalizedSignature = CursorUtil.getColumnIndexOrThrow(_cursor, "normalizedSignature");
          final int _cursorIndexOfImmediate = CursorUtil.getColumnIndexOrThrow(_cursor, "immediate");
          final int _cursorIndexOfLongTerm = CursorUtil.getColumnIndexOrThrow(_cursor, "longTerm");
          final int _cursorIndexOfProximity = CursorUtil.getColumnIndexOrThrow(_cursor, "proximity");
          final int _cursorIndexOfAccumulation = CursorUtil.getColumnIndexOrThrow(_cursor, "accumulation");
          final int _cursorIndexOfEffort = CursorUtil.getColumnIndexOrThrow(_cursor, "effort");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfIsAiGenerated = CursorUtil.getColumnIndexOrThrow(_cursor, "isAiGenerated");
          final int _cursorIndexOfNeedsClarification = CursorUtil.getColumnIndexOrThrow(_cursor, "needsClarification");
          final int _cursorIndexOfNeedsReEvaluation = CursorUtil.getColumnIndexOrThrow(_cursor, "needsReEvaluation");
          final int _cursorIndexOfIsManuallyPromoted = CursorUtil.getColumnIndexOrThrow(_cursor, "isManuallyPromoted");
          final int _cursorIndexOfIsDailyChore = CursorUtil.getColumnIndexOrThrow(_cursor, "isDailyChore");
          final int _cursorIndexOfEntryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "entryCount");
          final int _cursorIndexOfCreationTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "creationTimestamp");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfParentId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentId");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final Task _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRawText;
            _tmpRawText = _cursor.getString(_cursorIndexOfRawText);
            final String _tmpNormalizedSignature;
            _tmpNormalizedSignature = _cursor.getString(_cursorIndexOfNormalizedSignature);
            final float _tmpImmediate;
            _tmpImmediate = _cursor.getFloat(_cursorIndexOfImmediate);
            final float _tmpLongTerm;
            _tmpLongTerm = _cursor.getFloat(_cursorIndexOfLongTerm);
            final float _tmpProximity;
            _tmpProximity = _cursor.getFloat(_cursorIndexOfProximity);
            final float _tmpAccumulation;
            _tmpAccumulation = _cursor.getFloat(_cursorIndexOfAccumulation);
            final float _tmpEffort;
            _tmpEffort = _cursor.getFloat(_cursorIndexOfEffort);
            final boolean _tmpIsDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp != 0;
            final boolean _tmpIsAiGenerated;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsAiGenerated);
            _tmpIsAiGenerated = _tmp_1 != 0;
            final boolean _tmpNeedsClarification;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNeedsClarification);
            _tmpNeedsClarification = _tmp_2 != 0;
            final boolean _tmpNeedsReEvaluation;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfNeedsReEvaluation);
            _tmpNeedsReEvaluation = _tmp_3 != 0;
            final boolean _tmpIsManuallyPromoted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsManuallyPromoted);
            _tmpIsManuallyPromoted = _tmp_4 != 0;
            final boolean _tmpIsDailyChore;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsDailyChore);
            _tmpIsDailyChore = _tmp_5 != 0;
            final int _tmpEntryCount;
            _tmpEntryCount = _cursor.getInt(_cursorIndexOfEntryCount);
            final long _tmpCreationTimestamp;
            _tmpCreationTimestamp = _cursor.getLong(_cursorIndexOfCreationTimestamp);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpParentId;
            if (_cursor.isNull(_cursorIndexOfParentId)) {
              _tmpParentId = null;
            } else {
              _tmpParentId = _cursor.getString(_cursorIndexOfParentId);
            }
            final long _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getLong(_cursorIndexOfOrderIndex);
            _result = new Task(_tmpId,_tmpRawText,_tmpNormalizedSignature,_tmpImmediate,_tmpLongTerm,_tmpProximity,_tmpAccumulation,_tmpEffort,_tmpIsDone,_tmpIsAiGenerated,_tmpNeedsClarification,_tmpNeedsReEvaluation,_tmpIsManuallyPromoted,_tmpIsDailyChore,_tmpEntryCount,_tmpCreationTimestamp,_tmpNotes,_tmpParentId,_tmpOrderIndex);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object findActiveTaskBySignature(final String sig,
      final Continuation<? super Task> $completion) {
    final String _sql = "SELECT * FROM tasks WHERE normalizedSignature = ? AND isDone = 0 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sig);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Task>() {
      @Override
      @Nullable
      public Task call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRawText = CursorUtil.getColumnIndexOrThrow(_cursor, "rawText");
          final int _cursorIndexOfNormalizedSignature = CursorUtil.getColumnIndexOrThrow(_cursor, "normalizedSignature");
          final int _cursorIndexOfImmediate = CursorUtil.getColumnIndexOrThrow(_cursor, "immediate");
          final int _cursorIndexOfLongTerm = CursorUtil.getColumnIndexOrThrow(_cursor, "longTerm");
          final int _cursorIndexOfProximity = CursorUtil.getColumnIndexOrThrow(_cursor, "proximity");
          final int _cursorIndexOfAccumulation = CursorUtil.getColumnIndexOrThrow(_cursor, "accumulation");
          final int _cursorIndexOfEffort = CursorUtil.getColumnIndexOrThrow(_cursor, "effort");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfIsAiGenerated = CursorUtil.getColumnIndexOrThrow(_cursor, "isAiGenerated");
          final int _cursorIndexOfNeedsClarification = CursorUtil.getColumnIndexOrThrow(_cursor, "needsClarification");
          final int _cursorIndexOfNeedsReEvaluation = CursorUtil.getColumnIndexOrThrow(_cursor, "needsReEvaluation");
          final int _cursorIndexOfIsManuallyPromoted = CursorUtil.getColumnIndexOrThrow(_cursor, "isManuallyPromoted");
          final int _cursorIndexOfIsDailyChore = CursorUtil.getColumnIndexOrThrow(_cursor, "isDailyChore");
          final int _cursorIndexOfEntryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "entryCount");
          final int _cursorIndexOfCreationTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "creationTimestamp");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfParentId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentId");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final Task _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRawText;
            _tmpRawText = _cursor.getString(_cursorIndexOfRawText);
            final String _tmpNormalizedSignature;
            _tmpNormalizedSignature = _cursor.getString(_cursorIndexOfNormalizedSignature);
            final float _tmpImmediate;
            _tmpImmediate = _cursor.getFloat(_cursorIndexOfImmediate);
            final float _tmpLongTerm;
            _tmpLongTerm = _cursor.getFloat(_cursorIndexOfLongTerm);
            final float _tmpProximity;
            _tmpProximity = _cursor.getFloat(_cursorIndexOfProximity);
            final float _tmpAccumulation;
            _tmpAccumulation = _cursor.getFloat(_cursorIndexOfAccumulation);
            final float _tmpEffort;
            _tmpEffort = _cursor.getFloat(_cursorIndexOfEffort);
            final boolean _tmpIsDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp != 0;
            final boolean _tmpIsAiGenerated;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsAiGenerated);
            _tmpIsAiGenerated = _tmp_1 != 0;
            final boolean _tmpNeedsClarification;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNeedsClarification);
            _tmpNeedsClarification = _tmp_2 != 0;
            final boolean _tmpNeedsReEvaluation;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfNeedsReEvaluation);
            _tmpNeedsReEvaluation = _tmp_3 != 0;
            final boolean _tmpIsManuallyPromoted;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsManuallyPromoted);
            _tmpIsManuallyPromoted = _tmp_4 != 0;
            final boolean _tmpIsDailyChore;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsDailyChore);
            _tmpIsDailyChore = _tmp_5 != 0;
            final int _tmpEntryCount;
            _tmpEntryCount = _cursor.getInt(_cursorIndexOfEntryCount);
            final long _tmpCreationTimestamp;
            _tmpCreationTimestamp = _cursor.getLong(_cursorIndexOfCreationTimestamp);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpParentId;
            if (_cursor.isNull(_cursorIndexOfParentId)) {
              _tmpParentId = null;
            } else {
              _tmpParentId = _cursor.getString(_cursorIndexOfParentId);
            }
            final long _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getLong(_cursorIndexOfOrderIndex);
            _result = new Task(_tmpId,_tmpRawText,_tmpNormalizedSignature,_tmpImmediate,_tmpLongTerm,_tmpProximity,_tmpAccumulation,_tmpEffort,_tmpIsDone,_tmpIsAiGenerated,_tmpNeedsClarification,_tmpNeedsReEvaluation,_tmpIsManuallyPromoted,_tmpIsDailyChore,_tmpEntryCount,_tmpCreationTimestamp,_tmpNotes,_tmpParentId,_tmpOrderIndex);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DailyChore>> getDailyChoreTemplates() {
    final String _sql = "SELECT `daily_chores`.`id` AS `id`, `daily_chores`.`text` AS `text` FROM daily_chores";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_chores"}, new Callable<List<DailyChore>>() {
      @Override
      @NonNull
      public List<DailyChore> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfText = 1;
          final List<DailyChore> _result = new ArrayList<DailyChore>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyChore _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            _item = new DailyChore(_tmpId,_tmpText);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getDailyChoreTemplatesOneShot(
      final Continuation<? super List<DailyChore>> $completion) {
    final String _sql = "SELECT `daily_chores`.`id` AS `id`, `daily_chores`.`text` AS `text` FROM daily_chores";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailyChore>>() {
      @Override
      @NonNull
      public List<DailyChore> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfText = 1;
          final List<DailyChore> _result = new ArrayList<DailyChore>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyChore _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            _item = new DailyChore(_tmpId,_tmpText);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object hasDailyChoreForToday(final String text, final long startOfDay,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM tasks WHERE isDailyChore = 1 AND rawText = ? AND (isDone = 0 OR creationTimestamp >= ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, text);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startOfDay);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
