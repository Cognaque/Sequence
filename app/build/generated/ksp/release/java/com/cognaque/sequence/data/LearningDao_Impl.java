package com.cognaque.sequence.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LearningDao_Impl implements LearningDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<KeywordWeight> __insertionAdapterOfKeywordWeight;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllWeights;

  private final SharedSQLiteStatement __preparedStmtOfPruneExcessKeywords;

  public LearningDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfKeywordWeight = new EntityInsertionAdapter<KeywordWeight>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `keyword_weights` (`keyword`,`count`,`avgImm`,`avgLt`,`avgProx`,`avgAcc`,`avgEff`,`lastUpdated`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final KeywordWeight entity) {
        statement.bindString(1, entity.getKeyword());
        statement.bindLong(2, entity.getCount());
        statement.bindDouble(3, entity.getAvgImm());
        statement.bindDouble(4, entity.getAvgLt());
        statement.bindDouble(5, entity.getAvgProx());
        statement.bindDouble(6, entity.getAvgAcc());
        statement.bindDouble(7, entity.getAvgEff());
        statement.bindLong(8, entity.getLastUpdated());
      }
    };
    this.__preparedStmtOfDeleteAllWeights = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM keyword_weights";
        return _query;
      }
    };
    this.__preparedStmtOfPruneExcessKeywords = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM keyword_weights WHERE keyword NOT IN (SELECT keyword FROM keyword_weights ORDER BY count DESC, lastUpdated DESC LIMIT ?)";
        return _query;
      }
    };
  }

  @Override
  public Object saveWeight(final KeywordWeight weight,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfKeywordWeight.insert(weight);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllWeights(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllWeights.acquire();
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
          __preparedStmtOfDeleteAllWeights.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object pruneExcessKeywords(final int limit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPruneExcessKeywords.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, limit);
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
          __preparedStmtOfPruneExcessKeywords.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getWeight(final String keyword,
      final Continuation<? super KeywordWeight> $completion) {
    final String _sql = "SELECT * FROM keyword_weights WHERE keyword = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, keyword);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<KeywordWeight>() {
      @Override
      @Nullable
      public KeywordWeight call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfKeyword = CursorUtil.getColumnIndexOrThrow(_cursor, "keyword");
          final int _cursorIndexOfCount = CursorUtil.getColumnIndexOrThrow(_cursor, "count");
          final int _cursorIndexOfAvgImm = CursorUtil.getColumnIndexOrThrow(_cursor, "avgImm");
          final int _cursorIndexOfAvgLt = CursorUtil.getColumnIndexOrThrow(_cursor, "avgLt");
          final int _cursorIndexOfAvgProx = CursorUtil.getColumnIndexOrThrow(_cursor, "avgProx");
          final int _cursorIndexOfAvgAcc = CursorUtil.getColumnIndexOrThrow(_cursor, "avgAcc");
          final int _cursorIndexOfAvgEff = CursorUtil.getColumnIndexOrThrow(_cursor, "avgEff");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final KeywordWeight _result;
          if (_cursor.moveToFirst()) {
            final String _tmpKeyword;
            _tmpKeyword = _cursor.getString(_cursorIndexOfKeyword);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final float _tmpAvgImm;
            _tmpAvgImm = _cursor.getFloat(_cursorIndexOfAvgImm);
            final float _tmpAvgLt;
            _tmpAvgLt = _cursor.getFloat(_cursorIndexOfAvgLt);
            final float _tmpAvgProx;
            _tmpAvgProx = _cursor.getFloat(_cursorIndexOfAvgProx);
            final float _tmpAvgAcc;
            _tmpAvgAcc = _cursor.getFloat(_cursorIndexOfAvgAcc);
            final float _tmpAvgEff;
            _tmpAvgEff = _cursor.getFloat(_cursorIndexOfAvgEff);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _result = new KeywordWeight(_tmpKeyword,_tmpCount,_tmpAvgImm,_tmpAvgLt,_tmpAvgProx,_tmpAvgAcc,_tmpAvgEff,_tmpLastUpdated);
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
  public Object getWeightsForKeywords(final List<String> keywords,
      final Continuation<? super List<KeywordWeight>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM keyword_weights WHERE keyword IN (");
    final int _inputSize = keywords.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : keywords) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<KeywordWeight>>() {
      @Override
      @NonNull
      public List<KeywordWeight> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfKeyword = CursorUtil.getColumnIndexOrThrow(_cursor, "keyword");
          final int _cursorIndexOfCount = CursorUtil.getColumnIndexOrThrow(_cursor, "count");
          final int _cursorIndexOfAvgImm = CursorUtil.getColumnIndexOrThrow(_cursor, "avgImm");
          final int _cursorIndexOfAvgLt = CursorUtil.getColumnIndexOrThrow(_cursor, "avgLt");
          final int _cursorIndexOfAvgProx = CursorUtil.getColumnIndexOrThrow(_cursor, "avgProx");
          final int _cursorIndexOfAvgAcc = CursorUtil.getColumnIndexOrThrow(_cursor, "avgAcc");
          final int _cursorIndexOfAvgEff = CursorUtil.getColumnIndexOrThrow(_cursor, "avgEff");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final List<KeywordWeight> _result = new ArrayList<KeywordWeight>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KeywordWeight _item_1;
            final String _tmpKeyword;
            _tmpKeyword = _cursor.getString(_cursorIndexOfKeyword);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final float _tmpAvgImm;
            _tmpAvgImm = _cursor.getFloat(_cursorIndexOfAvgImm);
            final float _tmpAvgLt;
            _tmpAvgLt = _cursor.getFloat(_cursorIndexOfAvgLt);
            final float _tmpAvgProx;
            _tmpAvgProx = _cursor.getFloat(_cursorIndexOfAvgProx);
            final float _tmpAvgAcc;
            _tmpAvgAcc = _cursor.getFloat(_cursorIndexOfAvgAcc);
            final float _tmpAvgEff;
            _tmpAvgEff = _cursor.getFloat(_cursorIndexOfAvgEff);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _item_1 = new KeywordWeight(_tmpKeyword,_tmpCount,_tmpAvgImm,_tmpAvgLt,_tmpAvgProx,_tmpAvgAcc,_tmpAvgEff,_tmpLastUpdated);
            _result.add(_item_1);
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
