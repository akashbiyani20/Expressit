# Keep Room generated code intact
-keep class com.expressit.journal.data.** { *; }

# Whisper JNI bridge — names must survive minification
-keep class com.whispercpp.** { *; }
