# 📱 Airtel Money Printer — Guide Complet

Application Android pour TPE **Lenvii V2/V3/V2ii** sous Android 8.1/11.  
Impression automatique des SMS de transaction Airtel Money (sender `161`) via imprimante thermique 58mm ESC/POS.

---

## 📁 Structure du projet

```
AirtelMoneyPrinter/
├── app/
│   ├── src/main/
│   │   ├── java/com/airtel/moneyprinter/
│   │   │   ├── AirtelMoneyApp.java          ← Application principale
│   │   │   ├── parser/
│   │   │   │   └── AirtelSmsParser.java     ← Parseur SMS Airtel Money
│   │   │   ├── data/
│   │   │   │   ├── model/AirtelTransaction.java
│   │   │   │   ├── db/AppDatabase.java
│   │   │   │   ├── db/TransactionDao.java
│   │   │   │   └── repository/TransactionRepository.java
│   │   │   ├── printer/
│   │   │   │   └── PrinterManager.java      ← Gestion ESC/POS Lenvii
│   │   │   ├── receiver/
│   │   │   │   ├── SmsReceiver.java         ← Intercepteur SMS
│   │   │   │   └── BootReceiver.java        ← Démarrage auto
│   │   │   ├── service/
│   │   │   │   └── PrintForegroundService.java ← Service de fond
│   │   │   └── ui/
│   │   │       ├── main/MainActivity.java
│   │   │       ├── main/MainViewModel.java
│   │   │       ├── history/HistoryActivity.java
│   │   │       ├── history/TransactionAdapter.java
│   │   │       └── settings/SettingsActivity.java
│   │   ├── res/
│   │   │   ├── layout/       ← Tous les layouts XML
│   │   │   ├── values/       ← Couleurs, chaînes, thèmes
│   │   │   ├── drawable/     ← Logo Airtel Money
│   │   │   ├── xml/          ← preferences.xml
│   │   │   └── menu/         ← Menu toolbar
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
└── settings.gradle
```

---

## ⚙️ Étape 1 — Prérequis

| Outil | Version |
|-------|---------|
| Android Studio | Hedgehog 2023.1+ |
| JDK | 11 ou 17 |
| Android SDK | API 26–33 |
| Gradle | 7.4.2 |
| Connexion internet | Pour télécharger les dépendances (JitPack) |

---

## 📥 Étape 2 — Importer le projet

1. Ouvrir **Android Studio**
2. `File → Open` → sélectionner le dossier `AirtelMoneyPrinter/`
3. Attendre la synchronisation Gradle (première fois ~5 min)
4. Vérifier que `File → Project Structure → SDK Location` pointe vers votre SDK Android

---

## 🖼 Étape 3 — Ajouter le logo Airtel Money

Le logo est indispensable pour le ticket. Il doit être placé ici :

```
app/src/main/res/drawable/airtel_logo.png
```

**Spécifications :**
- Format : **PNG**
- Taille : **300 × 100 pixels** (largeur 300px max pour 58mm à 203dpi)
- Fond : **blanc ou transparent**
- Couleurs : rouge Airtel (`#E3001B`) sur fond blanc

> ⚠️ Si le logo n'est pas disponible, le ticket imprimera `AIRTEL MONEY` en texte.  
> Pour obtenir le logo officiel : contacter Airtel Congo/Gabon ou utiliser le logo publique sur le site officiel.

---

## 🔨 Étape 4 — Compilation

### Mode Debug (test rapide)

```bash
# Dans Android Studio Terminal
./gradlew assembleDebug
```

APK généré : `app/build/outputs/apk/debug/app-debug.apk`

### Mode Release (production)

**4.1. Créer une clé de signature :**

```bash
keytool -genkey -v \
  -keystore airtel_printer.jks \
  -alias airtel_key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 36500
```

**4.2. Ajouter dans `app/build.gradle` :**

```gradle
android {
    signingConfigs {
        release {
            storeFile file("airtel_printer.jks")
            storePassword "VOTRE_MOT_DE_PASSE"
            keyAlias "airtel_key"
            keyPassword "VOTRE_MOT_DE_PASSE"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
        }
    }
}
```

**4.3. Compiler :**

```bash
./gradlew assembleRelease
```

APK généré : `app/build/outputs/apk/release/app-release.apk`

---

## 📲 Étape 5 — Installation sur TPE Lenvii

### Via ADB (recommandé pour le déploiement)

```bash
# Connecter le TPE en USB + activer Débogage USB
adb devices                          # Vérifier la connexion
adb install -r app-release.apk      # Installer / mettre à jour
adb shell am start -n com.airtel.moneyprinter/.ui.main.MainActivity
```

### Via clé USB / carte SD

1. Copier l'APK sur une clé USB ou carte SD
2. Sur le TPE : **Paramètres → Sécurité → Sources inconnues → Activer**
3. Naviguer vers l'APK avec le gestionnaire de fichiers
4. Taper sur l'APK → Installer

---

## 🔐 Étape 6 — Permissions à accorder

Après installation, ouvrir l'application. Elle demandera automatiquement :

| Permission | Rôle |
|-----------|------|
| `RECEIVE_SMS` | Intercepter les SMS entrants |
| `READ_SMS` | Lire le contenu des SMS |
| `READ_PHONE_STATE` | Identifier l'état du téléphone |

**⚠️ IMPORTANT :** Sur certains TPE Lenvii sous Android 11, les permissions SMS doivent être accordées manuellement :

```
Paramètres → Applications → Airtel Money Printer → Permissions → SMS → Autoriser
```

### Activer le démarrage auto

Sur le TPE Lenvii, aller dans :
```
Paramètres → Applications → Airtel Money Printer → Autorisations spéciales
→ Démarrer en arrière-plan → Autoriser
→ Afficher en arrière-plan → Autoriser
```

---

## 🖨️ Étape 7 — Configuration de l'imprimante

### Ports série Lenvii (à tester dans cet ordre)

| Port | TPE |
|------|-----|
| `/dev/ttyS1` | Lenvii V3, V2ii (priorité 1) |
| `/dev/ttyS0` | Lenvii V2 (priorité 2) |
| `/dev/ttyUSB0` | Via USB-série |

- **Baudrate : 115200**
- L'application teste automatiquement tous les ports (mode "Auto")

### Choisir le port manuellement

Dans l'app : **Paramètres → Port imprimante** → sélectionner le port

### Diagnostic

Sur l'écran principal, tapper **"🔍 Diagnostic port série"** pour tester tous les ports et voir lequel répond.

---

## 🧪 Étape 8 — Test complet

### Test 1 : Impression de test
1. Ouvrir l'application
2. Tapper **"🖨 Tester l'impression"**
3. Un ticket test doit sortir de l'imprimante

### Test 2 : Simulation SMS Airtel Money
Via ADB, envoyer un SMS simulé :

```bash
# Simuler un SMS d'Airtel Money (sender 161)
adb shell am broadcast \
  -a android.provider.Telephony.SMS_RECEIVED \
  --es "pdus" "..." \
  com.airtel.moneyprinter/.receiver.SmsReceiver
```

Ou utiliser l'application **"SMS Gateway"** sur le TPE pour envoyer un SMS test depuis le numéro `161`.

### Test 3 : SMS réel
Envoyer un vrai SMS depuis le système Airtel Money vers le numéro SIM du TPE.

---

## 📋 Format du ticket imprimé

```
================================
      [LOGO AIRTEL MONEY]
================================
    NOTIFICATION TRANSACTION
--------------------------------

        ** RÉCEPTION **

MONTANT :
       25 000 FCFA

--------------------------------
Client       : Jean MBOULOU
Transaction  : TX123456
Date         : 15/01/2025
Heure        : 14:32:17
Nouveau solde: 120 000 FCFA
Tel          : +242 06 XXX XXX
--------------------------------

Message original :
Vous avez reçu 25 000 FCFA de
Jean MBOULOU. Nouveau solde :
120 000 FCFA. ID Transaction :
TX123456.

================================
  Merci d'utiliser Airtel Money
         www.airtel.com
================================
```

---

## 🔧 Personnalisation du ticket

Pour modifier le format du ticket, éditer la méthode `buildTicket()` dans :

```
app/src/main/java/com/airtel/moneyprinter/printer/PrinterManager.java
```

### Syntaxe ESC/POS DantSu

| Tag | Effet |
|-----|-------|
| `[L]` | Alignement gauche |
| `[C]` | Alignement centré |
| `[R]` | Alignement droite |
| `<b>...</b>` | Gras |
| `<u>...</u>` | Souligné |
| `<font size='big'>...</font>` | Double taille |
| `<img>BASE64</img>` | Image bitmap |

---

## 🐛 Débogage

### Voir les logs en temps réel

```bash
adb logcat -s "SmsReceiver" "PrinterManager" "PrintForegroundService" "AirtelSmsParser"
```

### Export des logs applicatifs

Les logs sont enregistrés dans :
```
/sdcard/Android/data/com.airtel.moneyprinter/files/airtel_printer.log
```

Récupérer via ADB :
```bash
adb pull /sdcard/Android/data/com.airtel.moneyprinter/files/airtel_printer.log ./
```

---

## ❗ Problèmes fréquents

| Problème | Solution |
|---------|---------|
| SMS non intercepté | Vérifier permission `RECEIVE_SMS` + activer démarrage fond |
| Impression échoue | Tester le diagnostic port série - essayer les 3 ports |
| App s'arrête au boot | Vérifier `RECEIVE_BOOT_COMPLETED` autorisé |
| Logo non imprimé | Ajouter `airtel_logo.png` dans `res/drawable/` |
| Doublon imprimé | Désactivé par hash SMS — si persistant, vider la base |
| Android 11 SMS bloqués | Aller dans Paramètres → Apps spéciales → Accès SMS |

---

## 📦 Dépendances utilisées

```gradle
// Base de données locale
implementation 'androidx.room:room-runtime:2.5.2'

// Imprimante ESC/POS (DantSu)
implementation 'com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.3.0'

// Material Design UI
implementation 'com.google.android.material:material:1.9.0'

// Architecture MVVM
implementation 'androidx.lifecycle:lifecycle-viewmodel:2.6.2'
implementation 'androidx.lifecycle:lifecycle-livedata:2.6.2'
```

---

## 🚀 Déploiement production

Pour un déploiement sur flotte de TPE Lenvii :

1. Compiler l'APK release signé
2. Utiliser une MDM (Mobile Device Management) ou ADB en masse
3. Configurer le port série correct via les paramètres de l'app
4. Vérifier que l'option "Sources inconnues" est activée sur tous les TPE
5. Tester sur 1 TPE avant déploiement général

---

## 📝 Notes Lenvii spécifiques

- **Lenvii V2** : Port `/dev/ttyS0`, baudrate 115200
- **Lenvii V3** : Port `/dev/ttyS1`, baudrate 115200  
- **Lenvii V2ii** : Port `/dev/ttyS1` ou `/dev/ttyS0` selon firmware
- L'application détecte automatiquement le bon port en mode "Auto"
- Les TPE Lenvii sous Android 11 peuvent nécessiter des permissions supplémentaires pour les ports série

---

*Airtel Money Printer v1.0 — Développé pour TPE Lenvii V2/V3/V2ii*
