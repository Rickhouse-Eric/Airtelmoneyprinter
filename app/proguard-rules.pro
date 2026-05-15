# Add project specific ProGuard rules here.

# Garder les classes ESC/POS DantSu
-keep class com.dantsu.escposprinter.** { *; }

# Garder les entités Room
-keep class com.airtel.moneyprinter.data.model.** { *; }

# Garder les DAOs Room
-keep interface com.airtel.moneyprinter.data.db.** { *; }

# AndroidX
-keep class androidx.room.** { *; }

# Garder les receivers et services
-keep class com.airtel.moneyprinter.receiver.** { *; }
-keep class com.airtel.moneyprinter.service.** { *; }
