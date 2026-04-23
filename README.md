Oke, kita rancang dulu secara jelas sebelum ngoding. Saya akan jelaskan UI, alur manajemen proyek, cara build, dan error handling-nya.

---

1. Tampilan UI / UX Aplikasi

Aplikasi akan pakai Bottom Navigation yang sudah ada di template (Home, Compiler, Profile). Tab "Compiler" adalah tempat utama fitur.

Tab Compiler — Tampilan Awal

```
┌─────────────────────────────┐
│  Compiler                   │
├─────────────────────────────┤
│ [ Install NDK ]             │  ← Jika NDK belum terpasang, tombol ini muncul
│ Status: ❌ Belum terinstall │
├─────────────────────────────┤
│ Source File:                │
│ ┌─────────────────────────┐ │
│ │ [ Pilih File .cpp    ]  │ │  ← Buka file picker untuk memilih file .cpp
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ (Kode C++ ditampilkan)  │ │  ← EditText besar read-only (atau editable)
│ │                         │ │
│ └─────────────────────────┘ │
│ [ Compile ]                 │  ← Tombol compile, hanya aktif jika NDK siap & file dipilih
├─────────────────────────────┤
│ Output:                     │
│ ┌─────────────────────────┐ │
│ │ (Log compile)           │ │  ← Scrollable TextView monospace
│ │                         │ │
│ └─────────────────────────┘ │
│ [ Simpan .so ]             │  ← Untuk menyimpan hasil ke folder Download
└─────────────────────────────┘
```

Catatan:

· EditText bisa readonly jika user hanya memilih file, atau editable jika user ingin menulis kode langsung di aplikasi. Kamu bisa buat dua mode: "Open File" atau "Write Code" (toggle). Biar sederhana, kita buat EditText editable + tombol "Open File" untuk mengisi dari file .cpp yang sudah ada.

---

2. Manajemen Proyek / Source Code

Ada beberapa opsi yang biasa dipakai di aplikasi compiler Android:

Opsi A: Single File (Paling Simpel, Cocok untuk Awal)

· User menulis kode C++ langsung di EditText, atau memilih satu file .cpp lewat ACTION_OPEN_DOCUMENT.
· Kode yang ditampilkan bisa diedit, lalu saat compile, kode disimpan ke file sementara di cache.
· Kelebihan: Tidak perlu manajemen folder, langsung jalan.
· Kekurangan: Tidak bisa multi-file.

Opsi B: Project Folder (Mirip AIDE)

· User memilih folder proyek lewat ACTION_OPEN_DOCUMENT_TREE. Aplikasi dapat akses baca/tulis ke folder itu dan subfoldernya.
· Di dalam folder, user bisa punya banyak file .cpp dan .h.
· Aplikasi menampilkan daftar file (mirip file explorer kecil). User pilih file mana yang akan di-compile (atau compile semua yang ada di folder).
· Output .so disimpan di folder yang sama (atau subfolder libs/).
· Kelebihan: Lebih profesional, mendukung banyak file, header, dan library.
· Kekurangan: Butuh UI tambahan (RecyclerView daftar file, navigasi folder).

Opsi C: Upload ZIP Proyek

· User membuat proyek di luar (misal di HP lain atau PC), zip folder proyek, lalu buka zip itu di aplikasi.
· Aplikasi ekstrak ke folder internal, lalu tampilkan daftar file seperti Opsi B.
· Mirip dengan "Import Project" di AIDE.

Rekomendasi: Mulai dari Opsi A dulu (single file), lalu setelah berhasil, tingkatkan ke Opsi B dengan project folder. Ini akan membuat UI lebih sederhana dan mudah di-debug. Kita akan pakai Opsi B nanti setelah fitur dasar oke.

---

3. Alur Build & Compile

Proses compile (setelah NDK terinstall dan kode siap):

1. User tekan tombol "Compile".
2. Aplikasi melakukan:
   · Ambil kode dari EditText (atau baca dari file project yang dipilih).
   · Tulis ke file sementara: cacheDir/temp.cpp.
   · Panggil CppCompiler.compile() dengan parameter file sumber tadi.
   · CppCompiler akan menjalankan perintah clang++ melalui ProcessBuilder.
3. Selama proses, log stdout/stderr ditangkap dan ditampilkan secara real-time di TextView log (bisa pakai LiveData atau Flow).
4. Jika sukses, file .so akan ada di folder output (misal cacheDir/liboutput.so). Log menunjukkan "Compile success" dan path output.
5. Jika gagal, log akan menampilkan error compiler lengkap dengan baris dan pesan error (seperti di terminal). Inilah root cause yang bisa dibaca user.
6. User bisa menyimpan file .so ke folder publik (misal Download) lewat ACTION_CREATE_DOCUMENT atau langsung copy ke folder proyek jika Opsi B.

---

4. Error Handling & Root Cause

Aplikasi harus bisa menampilkan error dengan jelas, mirip terminal GCC/Clang:

Contoh error yang mungkin:

```
temp.cpp:3:5: error: expected ';' after expression
    int a = 10
       ^
       ;
1 error generated.
```

Cara implementasi:

· ProcessBuilder diatur redirectErrorStream(true) agar stderr gabung ke stdout.
· Hasil output dibaca baris per baris, dikirim ke UI via LiveData<String> atau callback.
· Log akan tampil persis seperti di terminal. User bisa scroll.
· Jika exit code != 0, kita tambahkan pesan "Compile failed with exit code X".

Root cause akan terlihat dari pesan error compiler itu sendiri, jadi tidak perlu parsing manual yang rumit. Cukup tampilkan mentah.

---

5. Alur Install NDK

Sesuai permintaan, NDK tidak dibundle. User install dari ZIP:

1. User unduh file ZIP NDK (dari browser atau transfer).
2. Di aplikasi, tekan tombol "Install NDK", lalu pilih file ZIP lewat file picker (ACTION_OPEN_DOCUMENT).
3. Aplikasi mengekstrak ZIP ke context.filesDir/ndk/ (internal storage, tidak perlu izin ekstra).
4. Setelah ekstrak, aplikasi cek apakah struktur valid (ada toolchains/llvm/prebuilt/linux-<arch>/bin/clang++).
5. Status berubah jadi "✅ NDK terinstall".
6. Proses instalasi hanya sekali, kecuali user ingin ganti versi NDK dengan mengulangi langkah di atas (akan menimpa).

---

6. Integrasi dengan Template Awal

Template yang kamu punya sudah ada:

· MainActivity + BottomNav (Home, Dashboard, Profile). Bisa kita ubah DashboardFragment menjadi CompilerFragment.
· MainViewModel bisa kita perluas untuk log compile atau bikin CompilerViewModel terpisah (lebih rapi).
· Room dan Retrofit bisa diabaikan dulu, tidak dihapus tapi tidak dipakai. Nanti kalau sudah bersih, boleh dihapus.

File baru yang perlu ditambahkan:

· NdkManager.kt (object)
· CppCompiler.kt (class)
· CompilerFragment.kt (Fragment)
· Layout fragment_compiler.xml
· ViewModel CompilerViewModel.kt
· Ubah nav_graph.xml untuk mengganti fragment Dashboard dengan Compiler
· Ubah bottom_nav_menu.xml item id dan string.

Tidak perlu ubah izin di manifest karena akses file pakai SAF.

---

7. Pertanyaan Kamu: "Sync folder internal atau upload ZIP?"

Jawaban untuk fase awal:

· Tidak sync folder internal otomatis. Karena kita pakai Opsi A (Single File), user cukup buka file .cpp satu per satu atau tulis manual.
· Kalau mau lanjut ke Opsi B (Project Folder), user akan memilih folder proyek menggunakan ACTION_OPEN_DOCUMENT_TREE, lalu aplikasi akan punya akses persisten ke folder itu. Kita bisa simpan URI folder di SharedPreferences lalu setiap kali buka Compiler, kita muat daftar file dari folder tersebut (tidak perlu upload ZIP). Jadi user bisa mengelola file di folder itu menggunakan file manager HP, dan aplikasi akan otomatis melihat perubahan. Itu mirip "sync" dengan folder internal/eksternal.

Jadi untuk kemudahan pengembangan, kita mulai dari single file, lalu setelah semua lancar, kita tambahkan fitur project folder dengan penyimpanan URI SAF.

---

8. Visualisasi Alur Kompilasi Single File

```
User buka aplikasi
  ↓
Tab "Compiler"
  ↓
[Status NDK: Belum terinstall] → User klik Install NDK → pilih ZIP → ekstrak → status OK
  ↓
User tulis kode C++ di EditText ATAU klik "Open File" pilih .cpp
  ↓
User klik "Compile"
  ↓
Kode disimpan ke cache/temp.cpp
  ↓
CppCompiler dijalankan (clang++) dengan environment NDK
  ↓
Log muncul di TextView (live)
  ↓
Jika sukses → tampilkan "Success, output: liboutput.so", tombol "Save .so" muncul
Jika error → tampilkan error compiler, user bisa perbaiki kode
  ↓
User klik "Save .so" → pilih folder tujuan (SAF) → file tersimpan
```

---

Dengan rencana di atas, semua alur sudah jelas, UI sederhana tapi fungsional, error transparan, dan NDK diinstal dari luar. Kamu bisa langsung mulai modifikasi template-mu dengan tenang karena ada panduan.

Kalau kamu setuju dengan rancangan ini, kita bisa lanjut ke penulisan kode lengkapnya untuk NdkManager, CppCompiler, UI, dan ViewModel. Tapi kalau ada yang ingin diubah (misal langsung ingin project folder), bilang saja.