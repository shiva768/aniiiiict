# Gson でリフレクション変換するモデル。
# フィールド名がそのまま JSON キーになるもの（@SerializedName 無し）があるため、
# R8 にフィールドを削除・リネームさせない。
-keep class com.zelretch.aniiiiict.data.model.TokenResponse { *; }
-keep class com.zelretch.aniiiiict.data.model.MyAnimeList* { *; }

# スタックトレースを読めるように行番号を残す
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
